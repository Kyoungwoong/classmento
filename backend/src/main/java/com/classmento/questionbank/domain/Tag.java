package com.classmento.questionbank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  public static Tag of(String name) {
    throw new UnsupportedOperationException("TODO");
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
