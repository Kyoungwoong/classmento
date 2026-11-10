package com.classmento.interview.application;

import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.Evaluation;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.SessionStatus;
import com.classmento.speech.SttClient;
import com.classmento.speech.Transcript;
import com.classmento.storage.FileStorage;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 답변 하나를 백그라운드에서 처리한다: 녹음 → STT → (혼합이 아니거나 면접이 끝났으면) 평가.
 *
 * <p>외부 호출(STT·LLM)은 트랜잭션 밖에서, 결과 저장은 잠근 뒤 짧은 트랜잭션에서 한다. 저장할 때 {@code audioVersion}이 시작할 때와 다르면(그
 * 사이 다시 녹음함) 결과를 버린다.
 */
@Component
public class AnswerProcessor {

  static final String STT_FAILED = "답변을 글자로 바꾸지 못했어요. 다시 시도해 주세요.";
  static final String SILENT = "답변이 들리지 않았어요. 다시 녹음해 주세요.";
  static final String EVAL_FAILED = "답변을 평가하지 못했어요. 다시 시도해 주세요.";
  static final String UNEXPECTED = "답변을 처리하지 못했어요. 다시 시도해 주세요.";
  private static final Logger log = LoggerFactory.getLogger(AnswerProcessor.class);

  private final AnswerRepository answers;
  private final InterviewSessionRepository sessions;
  private final FileStorage storage;
  private final SttClient stt;
  private final AnswerEvaluator evaluator;
  private final TransactionTemplate tx;
  private final ReportScheduler reports;
  private final FollowUpService followUps;
  private final TaskExecutor interviewExecutor;

  public AnswerProcessor(
      AnswerRepository answers,
      InterviewSessionRepository sessions,
      FileStorage storage,
      SttClient stt,
      AnswerEvaluator evaluator,
      TransactionTemplate tx,
      ReportScheduler reports,
      FollowUpService followUps,
      TaskExecutor interviewExecutor) {
    this.answers = answers;
    this.sessions = sessions;
    this.storage = storage;
    this.stt = stt;
    this.evaluator = evaluator;
    this.tx = tx;
    this.reports = reports;
    this.followUps = followUps;
    this.interviewExecutor = interviewExecutor;
  }

  /** 어떤 예외가 나도 답변을 처리 중 상태로 남기지 않는다(FAILED로 두어 다시 시도할 수 있게). */
  public void process(Long answerId, int version) {
    try {
      run(answerId, version);
    } catch (RuntimeException e) {
      log.error("답변 {} 처리 중 예상하지 못한 오류: {}", answerId, e.getClass().getSimpleName());
      markFailed(answerId, version, UNEXPECTED);
    }
  }

  /** 처리를 시작하지 못했을 때(실행기 거절 등) 실패로 남긴다. */
  public void markFailed(Long answerId, int version, String reason) {
    try {
      save(answerId, version, a -> a.failed(reason));
    } catch (RuntimeException e) {
      log.error("답변 {} 실패 기록도 못 함: {}", answerId, e.getClass().getSimpleName());
    }
  }

  private void run(Long answerId, int version) {
    Answer answer = answers.findById(answerId).orElse(null);
    if (answer == null || answer.getAudioVersion() != version) {
      return;
    }
    if (answer.getStatus() == AnswerStatus.UPLOADED && !transcribe(answer, version)) {
      return;
    }
    // 다음 질문(꼬리)을 먼저 정하고, 평가는 평가 실행기로 넘긴다(다음 질문이 평가에 밀리지 않게)
    followUps.decide(answerId, version);
    try {
      interviewExecutor.execute(() -> evaluateSafely(answerId, version));
    } catch (TaskRejectedException e) {
      markFailed(answerId, version, EVAL_FAILED);
    }
  }

  private void evaluateSafely(Long answerId, int version) {
    try {
      evaluateIfDue(answerId, version);
    } catch (RuntimeException e) {
      log.error("답변 {} 평가 중 예상하지 못한 오류: {}", answerId, e.getClass().getSimpleName());
      markFailed(answerId, version, UNEXPECTED);
    }
  }

  /** 혼합 면접은 끝낸 뒤에만 평가한다(그 전에는 글자까지). */
  public void evaluateIfDue(Long answerId, int version) {
    Answer answer = answers.findById(answerId).orElse(null);
    if (answer == null
        || answer.getAudioVersion() != version
        || answer.getStatus() != AnswerStatus.TRANSCRIBED) {
      return;
    }
    InterviewSession session = sessions.findById(answer.getSessionId()).orElseThrow();
    if (session.getType() == InterviewType.MIXED
        && session.getStatus() == SessionStatus.IN_PROGRESS) {
      return;
    }
    Evaluation evaluation;
    try {
      evaluation = evaluator.evaluate(session.question(answer.getSeq()), answer.getTranscript());
    } catch (RuntimeException e) {
      log.warn("답변 {} 평가 실패: {}", answerId, e.getClass().getSimpleName());
      save(answerId, version, a -> a.failed(EVAL_FAILED));
      return;
    }
    if (save(answerId, version, a -> a.evaluated(evaluation))) {
      reports.request(answer.getSessionId());
    }
  }

  private boolean transcribe(Answer answer, int version) {
    Transcript transcript;
    try {
      byte[] audio = storage.load(answer.getAudioKey());
      transcript = stt.transcribe(audio, "answer-" + answer.getSeq());
    } catch (RuntimeException e) {
      log.warn("답변 {} STT 실패: {}", answer.getId(), e.getClass().getSimpleName());
      save(answer.getId(), version, a -> a.failed(STT_FAILED));
      return false;
    }
    if (transcript.text() == null || transcript.text().isBlank()) {
      save(answer.getId(), version, a -> a.failed(SILENT));
      return false;
    }
    return save(
        answer.getId(),
        version,
        a -> a.transcribed(transcript.text(), transcript.durationMillis()));
  }

  /** 잠그고 다시 읽어 버전이 같을 때만 바꾼다. 바꿨으면 true. */
  private boolean save(Long answerId, int version, Consumer<Answer> change) {
    return Boolean.TRUE.equals(
        tx.execute(
            s -> {
              Answer a = answers.lockById(answerId).orElse(null);
              if (a == null || a.getAudioVersion() != version) {
                return false;
              }
              change.accept(a);
              return true;
            }));
  }
}
