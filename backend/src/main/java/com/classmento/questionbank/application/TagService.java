package com.classmento.questionbank.application;

import com.classmento.questionbank.domain.Tag;
import com.classmento.questionbank.domain.TagRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 태그 유스케이스(Notion식): 자동완성, 없으면 만들고 있으면 기존 것. */
@Service
public class TagService {

  static final int SUGGEST_LIMIT = 10;

  private final TagRepository tags;

  public TagService(TagRepository tags) {
    this.tags = tags;
  }

  @Transactional(readOnly = true)
  public List<Tag> suggest(String query) {
    String key = Tag.keyOf(query);
    if (key.isEmpty()) {
      return List.of();
    }
    return tags.search(escapeLike(key), SUGGEST_LIMIT);
  }

  /**
   * 같은 키의 태그가 있으면 그것을, 없으면 새로 만든다. 동시에 같은 태그를 만들면 유니크 제약에 걸린 쪽이 기존 것을 다시 읽는다. 이 메서드에는 트랜잭션을 걸지
   * 않는다(실패한 insert가 호출 전체를 롤백 전용으로 만들지 않게).
   */
  public Result findOrCreate(String name) {
    Tag candidate = Tag.of(name);
    return tags.findByKey(candidate.getKey())
        .map(existing -> new Result(existing, false))
        .orElseGet(
            () -> {
              try {
                return new Result(tags.save(candidate), true);
              } catch (DataIntegrityViolationException raced) {
                return new Result(tags.findByKey(candidate.getKey()).orElseThrow(), false);
              }
            });
  }

  /** LIKE 특수문자 이스케이프(검색어의 %, _를 글자 그대로). */
  static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  public record Result(Tag tag, boolean created) {}
}
