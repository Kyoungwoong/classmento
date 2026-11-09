package com.classmento.interview.application;

import com.classmento.common.web.ApiException;
import com.classmento.interview.domain.Conditions;
import com.classmento.interview.domain.Experience;
import com.classmento.interview.domain.ExperienceSource;
import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.JobRole;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.interview.domain.QuestionKind;
import com.classmento.interview.domain.QuestionSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 면접 만들기·조회 유스케이스. 질문은 만들 때 모두 정한다(ADR 0005, 실행계획 2026-11-09-interview-session). */
@Service
public class InterviewService {

  /** 경험 기반 면접에서 AI에 넘기는 질문 은행 후보 수 */
  private static final int CANDIDATES = 10;

  private final InterviewSessionRepository sessions;
  private final QuestionSource questions;
  private final ExperienceSource experiences;
  private final ExperienceQuestionWriter writer;

  public InterviewService(
      InterviewSessionRepository sessions,
      QuestionSource questions,
      ExperienceSource experiences,
      ExperienceQuestionWriter writer) {
    this.sessions = sessions;
    this.questions = questions;
    this.experiences = experiences;
    this.writer = writer;
  }

  /** 트랜잭션 없이: 경험 기반은 AI를 부르므로 외부 호출을 트랜잭션 밖에 둔다(저장은 save 한 번). */
  public InterviewSession create(Long userId, Command cmd) {
    Conditions c =
        new Conditions(
            cmd.level(), cmd.jobRole(), cmd.resumeId(), cmd.experienceId(), cmd.narrowByResume());
    InterviewSession.requireValid(cmd.type(), cmd.count(), c);
    List<QuestionDraft> drafts =
        switch (cmd.type()) {
          case TECH -> tech(userId, c, cmd.count());
          case BEHAVIORAL -> behavioral(cmd.count());
          case MIXED -> mixed(c, cmd.count());
          case EXPERIENCE -> experience(userId, c, cmd.count());
        };
    return sessions.save(InterviewSession.start(userId, cmd.type(), cmd.count(), c, drafts));
  }

  @Transactional(readOnly = true)
  public InterviewSession get(Long userId, Long id) {
    return sessions.findByIdAndUserId(id, userId).orElseThrow(ApiException::notFound);
  }

  @Transactional(readOnly = true)
  public List<InterviewSession> list(Long userId) {
    return sessions.findByUserIdOrderByCreatedAtDescIdDesc(userId);
  }

  private List<QuestionDraft> tech(Long userId, Conditions c, int count) {
    Set<String> prefer =
        c.narrowByResume() ? experiences.resumeSkills(userId, c.resumeId()) : Set.of();
    return questions.pick(QuestionKind.TECH, c.level(), c.jobRole(), prefer, count);
  }

  /** 경험 임베딩으로 질문 은행 kNN 후보를 뽑고, 경험 원문과 함께 AI가 질문을 만든다. */
  private List<QuestionDraft> experience(Long userId, Conditions c, int count) {
    Experience exp = experiences.experience(userId, c.resumeId(), c.experienceId());
    List<QuestionDraft> candidates = questions.nearestTech(exp.embedding(), CANDIDATES);
    return writer.write(exp, candidates, count);
  }

  private List<QuestionDraft> behavioral(int count) {
    return questions.pick(QuestionKind.BEHAVIORAL, null, null, Set.of(), count);
  }

  /** 기술 ⌈N/2⌉ + 인성 ⌊N/2⌋, 기술부터 번갈아. 한쪽이 모자라면 전체가 모자라 시작이 거절된다. */
  private List<QuestionDraft> mixed(Conditions c, int count) {
    List<QuestionDraft> tech =
        questions.pick(QuestionKind.TECH, c.level(), c.jobRole(), Set.of(), (count + 1) / 2);
    List<QuestionDraft> behavioral = behavioral(count / 2);
    if (tech.size() < (count + 1) / 2 || behavioral.size() < count / 2) {
      return List.of();
    }
    List<QuestionDraft> out = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      out.add(i % 2 == 0 ? tech.get(i / 2) : behavioral.get(i / 2));
    }
    return out;
  }

  public record Command(
      InterviewType type,
      int count,
      InterviewLevel level,
      JobRole jobRole,
      Long resumeId,
      Long experienceId,
      boolean narrowByResume) {}
}
