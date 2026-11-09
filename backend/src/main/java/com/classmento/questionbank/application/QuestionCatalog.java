package com.classmento.questionbank.application;

import com.classmento.questionbank.domain.JobRole;
import com.classmento.questionbank.domain.Level;
import com.classmento.questionbank.domain.QuestionCandidateRepository;
import com.classmento.questionbank.domain.QuestionPicker;
import com.classmento.questionbank.domain.QuestionPicker.Candidate;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 질문 은행의 공개 조회 서비스(다른 컨텍스트용). 면접 컨텍스트가 infrastructure 어댑터를 통해 부른다. 값은 문자열(enum 이름)로 주고받는다 — 질문 은행의
 * 도메인 타입을 밖으로 내보내지 않기 위해서다.
 */
@Service
public class QuestionCatalog {

  private final QuestionCandidateRepository candidates;
  private final Random random = new SecureRandom();

  public QuestionCatalog(QuestionCandidateRepository candidates) {
    this.candidates = candidates;
  }

  /**
   * 면접 질문 고르기. 인성이면 연차·직무를 보지 않는다. 모자라면 있는 만큼만 돌려준다.
   *
   * @param level NEW·JUNIOR·MIDDLE·SENIOR 또는 null
   * @param jobRole 직무 이름 또는 null
   * @param prefer 먼저 고를 이름(이력서 기술 등, 대소문자 무시)
   */
  @Transactional(readOnly = true)
  public List<Picked> pickForInterview(
      boolean behavioral, String level, String jobRole, Set<String> prefer, int count) {
    List<Candidate> pool =
        candidates.candidates(
            behavioral, behavioral || jobRole == null ? null : JobRole.valueOf(jobRole));
    Set<String> keys =
        prefer.stream().map(p -> p.strip().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    return QuestionPicker.pick(
            pool, behavioral || level == null ? null : Level.valueOf(level), keys, count, random)
        .stream()
        .map(c -> new Picked(c.id(), c.text(), c.intent()))
        .toList();
  }

  public record Picked(Long id, String text, String intent) {}
}
