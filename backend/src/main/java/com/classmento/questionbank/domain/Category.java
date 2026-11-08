package com.classmento.questionbank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  public static Category top(String name, int sortOrder) {
    throw new UnsupportedOperationException("TODO");
  }

  public static Category child(Category parent, String name, int sortOrder) {
    throw new UnsupportedOperationException("TODO");
  }

  public static String nameKey(String name) {
    throw new UnsupportedOperationException("TODO");
  }

  public void rename(String name) {
    throw new UnsupportedOperationException("TODO");
  }

  public void hide() {
    throw new UnsupportedOperationException("TODO");
  }

  public void show() {
    throw new UnsupportedOperationException("TODO");
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
