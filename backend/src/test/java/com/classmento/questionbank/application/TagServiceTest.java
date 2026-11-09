package com.classmento.questionbank.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.questionbank.domain.Tag;
import com.classmento.questionbank.domain.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** "없으면 만들기"의 예외 처리: 동시 생성이면 기존 것, 다른 제약 위반이면 원래 예외를 그대로. */
class TagServiceTest {

  /** save는 항상 제약 위반, findByKey는 처음에 비어 있다가 두 번째에 raceWinner를 돌려준다. */
  private static TagRepository failingSave(Tag raceWinner) {
    return new TagRepository() {
      private int lookups = 0;

      @Override
      public <S extends Tag> S save(S tag) {
        throw new DataIntegrityViolationException("violation");
      }

      @Override
      public Optional<Tag> findByKey(String key) {
        return lookups++ == 0 ? Optional.empty() : Optional.ofNullable(raceWinner);
      }

      @Override
      public List<Tag> search(String likeEscapedKey, int limit) {
        return List.of();
      }
    };
  }

  @Test
  void 동시에_같은_태그가_만들어졌으면_기존_것을_돌려준다() {
    Tag winner = Tag.of("JPA");
    TagService.Result r = new TagService(failingSave(winner)).findOrCreate("jpa");
    assertThat(r.tag()).isSameAs(winner);
    assertThat(r.created()).isFalse();
  }

  @Test
  void 동시_생성이_아닌_제약_위반이면_원래_예외를_던진다() {
    assertThatThrownBy(() -> new TagService(failingSave(null)).findOrCreate("jpa"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
