package com.classmento.interview.application;

import com.classmento.common.domain.DomainException;
import com.classmento.interview.application.FollowUpWriter.Context;
import com.classmento.interview.application.FollowUpWriter.Turn;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.FollowUpState;
import com.classmento.interview.domain.InterviewQuestion;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.interview.domain.SessionStatus;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 답 하나를 듣고 꼬리질문을 붙일지 정한다(ADR 0008). 답변 처리에서 STT 직후, 평가보다 먼저 부른다.
 *
 * <ul>
 *   <li>짧은 답("모르겠습니다" 등)은 AI를 부르지 않고 닫는다
 *   <li>AI 호출은 트랜잭션 밖에서, 저장은 세션 → 답변 순서로 잠근 뒤(다시 녹음과 같은 순서) 상태와 녹음 버전을 다시 확인하고 한다. 그 사이 다시 녹음했거나 이미
 *       정했으면 결과를 버린다
 *   <li>어떤 실패도 밖으로 내지 않는다: 꼬리 없이 닫고 면접은 이어진다(답변을 실패로 만들지 않는다)
 * </ul>
 */
@Component
public class FollowUpService {

  /** 공백을 뺀 글자 수가 이보다 적으면 더 파고들 거리가 없다고 본다. */
  static final int SHORT_ANSWER = 30;

  /** 이보다 짧은 답에서만 "모르겠습니다"류를 포기로 본다(긴 답 속 "처음엔 모르겠어서…"는 제외). */
  private static final int GIVE_UP_WITHIN = 80;

  private static final Pattern GIVE_UP = Pattern.compile("모르겠|모릅니다|기억이안나|생각이안나");
  private static final Logger log = LoggerFactory.getLogger(FollowUpService.class);

  private final AnswerRepository answers;
  private final InterviewSessionRepository sessions;
  private final FollowUpWriter writer;
  private final TransactionTemplate tx;

  /** 지금 정하는 중인 답변(같은 답을 두 번 동시에 정하지 않게 — 복구 요청이 진행 중인 생성과 겹칠 때). */
  private final Set<Long> deciding = ConcurrentHashMap.newKeySet();

  public FollowUpService(
      AnswerRepository answers,
      InterviewSessionRepository sessions,
      FollowUpWriter writer,
      TransactionTemplate tx) {
    this.answers = answers;
    this.sessions = sessions;
    this.writer = writer;
    this.tx = tx;
  }

  /** 이 답(그 녹음 버전)에서 꼬리를 정한다. 대상이 아니거나 이미 정했으면 아무 일도 없다. */
  public void decide(Long answerId, int version) {
    if (!deciding.add(answerId)) {
      return;
    }
    long started = System.nanoTime();
    try {
      run(answerId, version);
    } catch (RuntimeException e) {
      log.warn("답변 {} 꼬리질문 판단 실패, 닫음: {}", answerId, e.getClass().getSimpleName());
      store(answerId, version, Optional.empty());
    } finally {
      deciding.remove(answerId);
      log.info("답변 {} 꼬리질문 판단 {}ms", answerId, (System.nanoTime() - started) / 1_000_000);
    }
  }

  /** 답은 있는데 아직 정하지 못한 질문(중간에 끊김)을 다시 정한다. */
  public void recover(Long sessionId) {
    InterviewSession s = sessions.findById(sessionId).orElse(null);
    if (s == null || s.getStatus() != SessionStatus.IN_PROGRESS) {
      return;
    }
    Map<Integer, Answer> bySeq = new HashMap<>();
    answers.findBySessionIdOrderBySeq(sessionId).forEach(a -> bySeq.put(a.getSeq(), a));
    Map<Integer, AnswerStatus> statuses = new HashMap<>();
    bySeq.forEach((seq, a) -> statuses.put(seq, a.getStatus()));
    for (int seq : s.awaitingFollowUps(statuses)) {
      Answer a = bySeq.get(seq);
      if (a.getStatus() == AnswerStatus.TRANSCRIBED || a.getStatus() == AnswerStatus.EVALUATED) {
        decide(a.getId(), a.getAudioVersion());
      }
    }
  }

  private void run(Long answerId, int version) {
    Answer answer = answers.findById(answerId).orElse(null);
    if (answer == null || answer.getAudioVersion() != version || !hasText(answer)) {
      return;
    }
    InterviewSession s = sessions.findById(answer.getSessionId()).orElse(null);
    if (s == null || s.getStatus() != SessionStatus.IN_PROGRESS) {
      return;
    }
    InterviewQuestion q = s.question(answer.getSeq());
    if (q.getFollowUp() != FollowUpState.AWAITING) {
      return;
    }
    if (isShort(answer.getTranscript())) {
      store(answerId, version, Optional.empty());
      return;
    }
    Optional<QuestionDraft> draft =
        writer.write(
            new Context(
                s.getType(), s.getJobRole(), s.getLevel(), s.getMaxFollowUps() - q.getDepth()),
            chain(s, q, answer));
    store(answerId, version, draft);
  }

  /** 본 질문부터 이 질문까지(질문·의도·답). 마지막 턴이 방금 받은 답이다. */
  private List<Turn> chain(InterviewSession s, InterviewQuestion last, Answer lastAnswer) {
    Map<Integer, String> transcripts = new HashMap<>();
    answers
        .findBySessionIdOrderBySeq(s.getId())
        .forEach(a -> transcripts.put(a.getSeq(), a.getTranscript()));
    transcripts.put(last.getSeq(), lastAnswer.getTranscript());
    Deque<Turn> turns = new ArrayDeque<>();
    InterviewQuestion q = last;
    while (q != null) {
      turns.addFirst(
          new Turn(q.getText(), q.getIntent(), transcripts.getOrDefault(q.getSeq(), "")));
      q = q.getParentSeq() == null ? null : s.question(q.getParentSeq());
    }
    return List.copyOf(turns);
  }

  /** 잠그고 다시 확인한 뒤 꼬리를 붙이거나(draft 있음) 닫는다. 그 사이 다시 녹음했거나 이미 정했으면 버린다. */
  private void store(Long answerId, int version, Optional<QuestionDraft> draft) {
    tx.executeWithoutResult(
        t -> {
          Answer peek = answers.findById(answerId).orElse(null);
          if (peek == null) {
            return;
          }
          InterviewSession s = sessions.lockById(peek.getSessionId()).orElse(null);
          Answer a = answers.lockById(answerId).orElse(null);
          if (s == null
              || a == null
              || s.getStatus() != SessionStatus.IN_PROGRESS
              || a.getAudioVersion() != version
              || !hasText(a)
              || s.question(a.getSeq()).getFollowUp() != FollowUpState.AWAITING) {
            return;
          }
          if (draft.isEmpty()) {
            s.closeFollowUp(a.getSeq());
            return;
          }
          try {
            s.addFollowUp(a.getSeq(), draft.get());
          } catch (DomainException e) {
            // 같은 문장 등: 꼬리 없이 넘어간다
            s.closeFollowUp(a.getSeq());
          }
        });
  }

  private static boolean hasText(Answer a) {
    return (a.getStatus() == AnswerStatus.TRANSCRIBED || a.getStatus() == AnswerStatus.EVALUATED)
        && a.getTranscript() != null
        && !a.getTranscript().isBlank();
  }

  static boolean isShort(String transcript) {
    String compact = transcript == null ? "" : transcript.replaceAll("\\s+", "");
    return compact.length() < SHORT_ANSWER
        || (compact.length() < GIVE_UP_WITHIN && GIVE_UP.matcher(compact).find());
  }
}
