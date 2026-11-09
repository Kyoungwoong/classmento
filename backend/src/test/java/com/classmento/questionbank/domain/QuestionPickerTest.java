package com.classmento.questionbank.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.questionbank.domain.QuestionPicker.Candidate;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/** 면접용 질문 고르기: 같은 연차 먼저·한 단계 아래까지, 이력서 기술과 맞는 것 먼저, 소분류가 겹치지 않게. */
class QuestionPickerTest {

  private static Candidate c(long id, Level level, long sub, String... keys) {
    return new Candidate(id, "질문 " + id, null, level, sub, Set.of(keys));
  }

  private static List<Long> ids(List<Candidate> picked) {
    return picked.stream().map(Candidate::id).toList();
  }

  @Test
  void 같은_연차를_먼저_고르고_모자라면_한_단계_아래까지만() {
    List<Candidate> pool =
        List.of(
            c(1, Level.JUNIOR, 10),
            c(2, Level.NEW, 11),
            c(3, Level.MIDDLE, 12),
            c(4, Level.SENIOR, 13));

    List<Candidate> picked = QuestionPicker.pick(pool, Level.JUNIOR, Set.of(), 3, new Random(1));

    assertThat(ids(picked)).containsExactly(1L, 2L); // MIDDLE·SENIOR는 고르지 않고, 모자라면 있는 만큼
  }

  @RepeatedTest(5)
  void 소분류가_겹치지_않게_먼저_고르고_모자라면_채운다() {
    List<Candidate> pool =
        List.of(c(1, Level.NEW, 10), c(2, Level.NEW, 10), c(3, Level.NEW, 11), c(4, Level.NEW, 12));

    List<Candidate> three = QuestionPicker.pick(pool, Level.NEW, Set.of(), 3, new Random());
    assertThat(three).extracting(Candidate::subcategoryId).doesNotHaveDuplicates();

    List<Candidate> four = QuestionPicker.pick(pool, Level.NEW, Set.of(), 4, new Random());
    assertThat(ids(four)).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
  }

  @RepeatedTest(5)
  void 이력서_기술과_맞는_질문을_먼저_고른다() {
    List<Candidate> pool =
        List.of(
            c(1, Level.JUNIOR, 10, "java"),
            c(2, Level.JUNIOR, 11, "python"),
            c(3, Level.JUNIOR, 12, "spring", "jpa"),
            c(4, Level.JUNIOR, 13, "go"));

    List<Candidate> picked =
        QuestionPicker.pick(pool, Level.JUNIOR, Set.of("jpa", "java"), 2, new Random());

    assertThat(ids(picked)).containsExactlyInAnyOrder(1L, 3L);
  }

  @Test
  void 연차가_없으면_모두_고를_수_있다() {
    List<Candidate> pool = List.of(c(1, Level.SENIOR, 10), c(2, Level.NEW, 11));
    assertThat(QuestionPicker.pick(pool, null, Set.of(), 2, new Random(1))).hasSize(2);
  }
}
