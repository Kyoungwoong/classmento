package com.classmento.interview.application;

import com.classmento.common.domain.DomainException;
import com.classmento.common.web.ApiException;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.storage.FileStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final ReportScheduler reports;

  public AnswerService(
      InterviewSessionRepository sessions,
      AnswerRepository answers,
      FileStorage storage,
      AnswerProcessor processor,
      TaskExecutor interviewExecutor,
      TransactionTemplate tx,
      ReportScheduler reports) {
    this.sessions = sessions;
    this.answers = answers;
    this.storage = storage;
    this.processor = processor;
    this.interviewExecutor = interviewExecutor;
    this.tx = tx;
    this.reports = reports;
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
    // 다시 녹음마다 새 파일(같은 이름을 덮어쓰면 처리 중인 예전 버전과 섞인다)
    String key =
        storage.store(
            "answers/" + userId + "/" + sessionId,
            "q" + seq + "-" + UUID.randomUUID() + extension(type),
            audio);
    Answer saved;
    try {
      saved = record(session, seq, key, type);
    } catch (DataIntegrityViolationException e) {
      // 같은 질문의 첫 답변이 동시에 두 번 오면 한쪽이 먼저 행을 만든다 → 그 행을 잠그고 다시 녹음으로
      saved = record(session, seq, key, type);
    }
    start(saved);
    return saved;
  }

  private Answer record(InterviewSession session, int seq, String key, String type) {
    return tx.execute(
        s -> {
          // 끝내기와 거의 동시에 온 다시 녹음이 끝난 뒤에 반영되지 않게 세션을 잠그고 다시 확인
          InterviewSession locked = sessions.lockById(session.getId()).orElseThrow();
          locked.requireAnswerable(seq);
          return answers
              .lockBySessionIdAndSeq(session.getId(), seq)
              .map(
                  a -> {
                    // 이 답을 근거로 꼬리질문을 이미 했으면 다시 녹음할 수 없다(ADR 0008)
                    locked.requireReRecordable(seq);
                    a.reRecord(key, type);
                    return a;
                  })
              .orElseGet(() -> answers.save(Answer.recorded(session, seq, key, type)));
        });
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

  /** 면접 끝내기: 모든 질문에 답했어야 한다. 끝나면 글자까지 된 답변(혼합)을 평가하고, 모두 평가되면 종합을 만든다. */
  public InterviewSession finish(Long userId, Long sessionId) {
    sessions.findByIdAndUserId(sessionId, userId).orElseThrow(ApiException::notFound);
    InterviewSession finished =
        tx.execute(
            s -> {
              InterviewSession locked = sessions.lockById(sessionId).orElseThrow();
              Map<Integer, AnswerStatus> answered = new HashMap<>();
              answers
                  .findBySessionIdOrderBySeq(sessionId)
                  .forEach(a -> answered.put(a.getSeq(), a.getStatus()));
              locked.finish(answered);
              return locked;
            });
    for (Answer a : answers.findBySessionIdOrderBySeq(sessionId)) {
      if (a.getStatus() == AnswerStatus.TRANSCRIBED) {
        start(a); // 평가부터(혼합). 거절되면 FAILED로 남아 다시 시도할 수 있다
      }
    }
    // 남은 평가가 끝날 때, 그리고 리포트를 조회할 때도 다시 요청된다(세션마다 하나만 진행)
    reports.request(sessionId);
    return finished;
  }

  public List<Answer> answersOf(Long sessionId) {
    return answers.findBySessionIdOrderBySeq(sessionId);
  }

  private void start(Answer a) {
    Long id = a.getId();
    int version = a.getAudioVersion();
    try {
      interviewExecutor.execute(() -> processor.process(id, version));
    } catch (TaskRejectedException e) {
      processor.markFailed(id, version, "지금은 답변을 처리할 수 없어요. 잠시 후 다시 시도해 주세요.");
    }
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

  /** "audio/webm;codecs=opus" → "audio/webm". 받는 형식(브라우저 녹음·흔한 오디오)이 아니면 400. */
  private static String audioType(String contentType) {
    String base =
        contentType == null ? "" : contentType.split(";")[0].strip().toLowerCase(Locale.ROOT);
    if (!EXTENSIONS.containsKey(base)) {
      throw ApiException.badRequest("녹음 파일(webm·ogg·m4a·wav·mp3)만 올릴 수 있어요.");
    }
    return base;
  }

  private static final Map<String, String> EXTENSIONS =
      Map.ofEntries(
          Map.entry("audio/webm", ".webm"),
          Map.entry("audio/ogg", ".ogg"),
          Map.entry("audio/mp4", ".m4a"),
          Map.entry("audio/m4a", ".m4a"),
          Map.entry("audio/x-m4a", ".m4a"),
          Map.entry("audio/aac", ".aac"),
          Map.entry("audio/wav", ".wav"),
          Map.entry("audio/x-wav", ".wav"),
          Map.entry("audio/wave", ".wav"),
          Map.entry("audio/mpeg", ".mp3"));

  private static String extension(String type) {
    return EXTENSIONS.get(type);
  }

  public record Audio(byte[] bytes, String contentType) {}
}
