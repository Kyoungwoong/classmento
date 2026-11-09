package com.classmento.questionbank.domain;

import com.classmento.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;

/** 질문 카테고리(대분류 → 소분류 2단계). 규칙: docs/conventions/ddd-tdd.md, 실행계획 2026-11-09-question-bank. */
@Entity
@Table(name = "qb_category")
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long parentId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private boolean active = true;

  protected Category() {}

  public static final int NAME_MAX = 30;

  /** 대분류 */
  public static Category top(String name, int sortOrder) {
    Category c = new Category();
    c.name = validName(name);
    c.sortOrder = sortOrder;
    return c;
  }

  /** 소분류. 부모는 보이는 대분류여야 한다. */
  public static Category child(Category parent, String name, int sortOrder) {
    if (!parent.isTop()) {
      throw new DomainException("CATEGORY_TOO_DEEP", "카테고리는 두 단계까지만 만들 수 있어요.");
    }
    if (!parent.active) {
      throw new DomainException("CATEGORY_HIDDEN", "숨긴 카테고리 아래에는 만들 수 없어요.");
    }
    Category c = top(name, sortOrder);
    c.parentId = parent.id;
    return c;
  }

  /** 같은 부모 아래 이름 중복 판단용: 공백 제거 + 소문자. */
  public static String nameKey(String name) {
    return name == null ? "" : name.replaceAll("(?U)\\s+", "").toLowerCase(Locale.ROOT);
  }

  public void rename(String name) {
    this.name = validName(name);
  }

  public void reorder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public void hide() {
    this.active = false;
  }

  public void show() {
    this.active = true;
  }

  private static String validName(String raw) {
    String n = raw == null ? "" : raw.strip();
    if (n.isEmpty() || n.length() > NAME_MAX) {
      throw new DomainException("INVALID_CATEGORY", "카테고리 이름은 1~30자로 입력해 주세요.");
    }
    return n;
  }

  public boolean isTop() {
    return parentId == null;
  }

  /** 테스트 전용: 저장 없이 id를 정한다. */
  void assignIdForTest(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public Long getParentId() {
    return parentId;
  }

  public String getName() {
    return name;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public boolean isActive() {
    return active;
  }
}
