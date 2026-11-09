package com.classmento.questionbank.domain;

import com.classmento.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;

/** 질문 태그(Notion식: 입력하며 찾고, 없으면 바로 만든다). key = 대소문자 무시 중복 판단용. */
@Entity
@Table(name = "qb_tag")
public class Tag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "name_key", nullable = false, unique = true)
  private String key;

  protected Tag() {}

  public static final int NAME_MAX = 20;

  public static Tag of(String raw) {
    String name = raw == null ? "" : raw.strip().replaceAll("(?U)\\s+", " ");
    if (name.isEmpty()
        || name.length() > NAME_MAX
        || keyOf(name).length() > NAME_MAX // 소문자 변환으로 길어지는 글자(예: İ) 대비
        || name.contains(",")) {
      throw new DomainException("INVALID_TAG", "태그는 쉼표 없이 1~20자로 입력해 주세요.");
    }
    Tag t = new Tag();
    t.name = name;
    t.key = keyOf(name);
    return t;
  }

  /** 대소문자 무시 중복 판단용 키. */
  public static String keyOf(String name) {
    return name == null ? "" : name.strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getKey() {
    return key;
  }
}
