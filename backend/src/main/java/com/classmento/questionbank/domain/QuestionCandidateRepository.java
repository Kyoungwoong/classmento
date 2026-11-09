package com.classmento.questionbank.domain;

import com.classmento.questionbank.domain.QuestionPicker.Candidate;
import java.util.List;

/** 면접에 낼 수 있는 질문 후보(보이는 질문 + 보이는 카테고리). 구현: infrastructure.JdbcQuestionCandidateRepository. */
public interface QuestionCandidateRepository {

  /**
   * @param behavioral true면 인성 대분류({@link Category#BEHAVIORAL_TOP})만, false면 그 밖의 대분류
   * @param jobRole null이 아니면 그 직무나 COMMON인 질문만
   */
  List<Candidate> candidates(boolean behavioral, JobRole jobRole);
}
