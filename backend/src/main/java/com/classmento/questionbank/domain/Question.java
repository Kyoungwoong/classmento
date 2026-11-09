package com.classmento.questionbank.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 질문 은행의 질문. 소분류에만 속하고, 직무·태그는 ID 집합으로 가진다(다른 애그리거트는 ID로만 참조). 임베딩 벡터는 JPA로 매핑하지 않는다
 * (infrastructure JDBC).
 */
@Entity
@Table(name = "qb_question")
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long categoryId;

  @Column(nullable = false)
  private String text;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Level level;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "qb_question_job_role", joinColumns = @JoinColumn(name = "question_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "job_role", nullable = false)
  private Set<JobRole> jobRoles = new LinkedHashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "qb_question_tag", joinColumns = @JoinColumn(name = "question_id"))
  @Column(name = "tag_id", nullable = false)
  private Set<Long> tagIds = new LinkedHashSet<>();

  private String intent;

  @Column(nullable = false)
  private boolean active = true;

  private Long createdBy;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  protected Question() {}

  public static Question create(
      Category top,
      Category sub,
      String text,
      Level level,
      Set<JobRole> jobRoles,
      Set<Long> tagIds,
      String intent,
      Long createdBy) {
    throw new UnsupportedOperationException("TODO");
  }

  public void edit(
      Category top,
      Category sub,
      String text,
      Level level,
      Set<JobRole> jobRoles,
      Set<Long> tagIds,
      String intent) {
    throw new UnsupportedOperationException("TODO");
  }

  public void hide() {
    throw new UnsupportedOperationException("TODO");
  }

  public void show() {
    throw new UnsupportedOperationException("TODO");
  }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public String getText() {
    return text;
  }

  public Level getLevel() {
    return level;
  }

  public Set<JobRole> getJobRoles() {
    return Set.copyOf(jobRoles);
  }

  public Set<Long> getTagIds() {
    return Set.copyOf(tagIds);
  }

  public String getIntent() {
    return intent;
  }

  public boolean isActive() {
    return active;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
