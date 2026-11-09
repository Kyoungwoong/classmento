package com.classmento.interview.application;

import com.classmento.common.domain.DomainException;
import com.classmento.common.web.ApiException;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.storage.FileStorage;
import java.util.List;
import java.util.Locale;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 답변 올리기·다시 녹음·다시 시도·녹음 듣기. STT·평가는 {@link AnswerProcessor}가 백그라운드에서 한다. */
@Service
public class AnswerService {

  public static final int MAX_BYTES = 10 * 1024 * 1024;

  private final InterviewSessionRepository sessions;
  private final AnswerRepository answers;
  private final FileStorage storage;
  private final AnswerProcessor processor;
  private final TaskExecutor interviewExecutor;
  private final TransactionTemplate tx;

  public AnswerService(
      InterviewSessionRepository sessions,
      AnswerRepository answers,
      FileStorage storage,
      AnswerProcessor processor,
      TaskExecutor interviewExecutor,
      TransactionTemplate tx) {
    this.sessions = sessions;
    this.answers = answers;
    this.storage = storage;
    this.processor = processor;
    this.interviewExecutor = interviewExecutor;
    this.tx = tx;
  }

  /** 녹음을 받아 저장하고 처리를 시작한다. 같은 질문에 다시 올리면 다시 녹음으로 본다. */
  public Answer upload(Long userId, Long sessionId, int seq, byte[] audio, String contentType) {
    String type = audioType(contentType);
    if (audio.length == 0) {
      throw ApiException.badRequest("녹음이 비어 있어요. 다시 녹음해 주세요.");
    }
    if (audio.length > MAX_BYTES) {
      throw ApiException.badRequest("녹음이 너무 길어요. 3분 안으로 답해 주세요.");
    }
    InterviewSession session = sessionWithQuestion(userId, sessionId, seq);
    session.requireAnswerable(seq);
    String key =
        storage.store("answers/" + userId + "/" + sessionId, "q" + seq + extension(type), audio);
    Answer saved =
        tx.execute(
            s ->
                answers
                    .lockBySessionIdAndSeq(sessionId, seq)
                    .map(
                        a -> {
                          a.reRecord(key, type);
                          return a;
                        })
                    .orElseGet(() -> answers.save(Answer.recorded(session, seq, key, type))));
    start(saved);
    return saved;
  }

  /** 실패한 답변을 같은 녹음으로 다시 처리한다. */
  public Answer retry(Long userId, Long sessionId, int seq) {
    sessionWithQuestion(userId, sessionId, seq);
    Answer saved =
        tx.execute(
            s -> {
              Answer a =
                  answers.lockBySessionIdAndSeq(sessionId, seq).orElseThrow(ApiException::notFound);
              a.retry();
              return a;
            });
    start(saved);
    return saved;
  }

  public Audio audio(Long userId, Long sessionId, int seq) {
    sessionWithQuestion(userId, sessionId, seq);
    Answer a = answers.findBySessionIdAndSeq(sessionId, seq).orElseThrow(ApiException::notFound);
    return new Audio(storage.load(a.getAudioKey()), a.getContentType());
  }

  public List<Answer> answersOf(Long sessionId) {
    return answers.findBySessionIdOrderBySeq(sessionId);
  }

  private void start(Answer a) {
    Long id = a.getId();
    int version = a.getAudioVersion();
    interviewExecutor.execute(() -> processor.process(id, version));
  }

  /** 본인 면접이고 그 번호의 질문이 있어야 한다(아니면 404). */
  private InterviewSession sessionWithQuestion(Long userId, Long sessionId, int seq) {
    InterviewSession session =
        sessions.findByIdAndUserId(sessionId, userId).orElseThrow(ApiException::notFound);
    try {
      session.question(seq);
    } catch (DomainException e) {
      throw ApiException.notFound();
    }
    return session;
  }

  /** "audio/webm;codecs=opus" → "audio/webm". 오디오가 아니면 400. */
  private static String audioType(String contentType) {
    String base =
        contentType == null ? "" : contentType.split(";")[0].strip().toLowerCase(Locale.ROOT);
    if (!base.startsWith("audio/")) {
      throw ApiException.badRequest("녹음 파일(오디오)만 올릴 수 있어요.");
    }
    return base;
  }

  private static String extension(String type) {
    return switch (type) {
      case "audio/webm" -> ".webm";
      case "audio/ogg" -> ".ogg";
      case "audio/mp4", "audio/m4a", "audio/x-m4a" -> ".m4a";
      case "audio/wav", "audio/x-wav", "audio/wave" -> ".wav";
      case "audio/mpeg" -> ".mp3";
      default -> ".audio";
    };
  }

  public record Audio(byte[] bytes, String contentType) {}
}
