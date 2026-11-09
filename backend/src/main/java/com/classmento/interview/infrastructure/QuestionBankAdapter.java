package com.classmento.interview.infrastructure;

import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.JobRole;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.interview.domain.QuestionKind;
import com.classmento.interview.domain.QuestionSource;
import com.classmento.questionbank.application.QuestionCatalog;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 질문 은행 컨텍스트로 가는 어댑터. 공개 조회 서비스 {@link QuestionCatalog}만 쓴다(ArchitectureTest). */
@Component
public class QuestionBankAdapter implements QuestionSource {

  private final QuestionCatalog catalog;

  public QuestionBankAdapter(QuestionCatalog catalog) {
    this.catalog = catalog;
  }

  @Override
  public List<QuestionDraft> pick(
      QuestionKind kind, InterviewLevel level, JobRole jobRole, Set<String> prefer, int count) {
    return catalog
        .pickForInterview(
            kind == QuestionKind.BEHAVIORAL,
            level == null ? null : level.name(),
            jobRole == null ? null : jobRole.name(),
            prefer,
            count)
        .stream()
        .map(p -> QuestionDraft.fromBank(p.id(), p.text(), p.intent(), kind))
        .toList();
  }

  @Override
  public List<QuestionDraft> nearestTech(float[] query, int k) {
    return catalog.nearestTech(query, k).stream()
        .map(p -> QuestionDraft.fromBank(p.id(), p.text(), p.intent(), QuestionKind.TECH))
        .toList();
  }
}
