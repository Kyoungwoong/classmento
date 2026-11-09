package com.classmento.questionbank.domain;

import com.classmento.common.domain.DomainException;
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

  public static final int TEXT_MIN = 5;
  public static final int TEXT_MAX = 300;
  public static final int TAGS_MAX = 10;
  public static final int INTENT_MAX = 200;

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
    requireOpenSubcategory(top, sub);
    Question q = new Question();
    q.apply(sub, text, level, jobRoles, tagIds, intent);
    q.createdBy = createdBy;
    return q;
  }

  /** 같은(숨겨진) 카테고리 안에서는 고칠 수 있지만, 다른 숨긴 카테고리로 옮길 수는 없다. */
  public void edit(
      Category top,
      Category sub,
      String text,
      Level level,
      Set<JobRole> jobRoles,
      Set<Long> tagIds,
      String intent) {
    if (sub.getId() != null && sub.getId().equals(categoryId)) {
      requireSubcategory(top, sub);
    } else {
      requireOpenSubcategory(top, sub);
    }
    apply(sub, text, level, jobRoles, tagIds, intent);
  }

  public void hide() {
    this.active = false;
  }

  public void show() {
    this.active = true;
  }

  private void apply(
      Category sub,
      String text,
      Level level,
      Set<JobRole> jobRoles,
      Set<Long> tagIds,
      String intent) {
    String t = text == null ? "" : text.strip();
    if (t.length() < TEXT_MIN || t.length() > TEXT_MAX) {
      throw new DomainException("INVALID_QUESTION", "질문은 5~300자로 입력해 주세요.");
    }
    if (jobRoles == null || jobRoles.isEmpty()) {
      throw new DomainException("QUESTION_JOB_ROLE_REQUIRED", "직무를 하나 이상 골라 주세요.");
    }
    Set<Long> tags = tagIds == null ? Set.of() : tagIds;
    if (tags.size() > TAGS_MAX) {
      throw new DomainException("TOO_MANY_TAGS", "태그는 10개까지 붙일 수 있어요.");
    }
    this.categoryId = sub.getId();
    this.text = t;
    this.level = level;
    this.jobRoles = new LinkedHashSet<>(jobRoles);
    this.tagIds = new LinkedHashSet<>(tags);
    this.intent = intent == null || intent.isBlank() ? null : intent.strip();
  }

  private static void requireSubcategory(Category top, Category sub) {
    if (sub.isTop() || !top.isTop() || !sub.getParentId().equals(top.getId())) {
      throw new DomainException("QUESTION_NEEDS_SUBCATEGORY", "질문은 소분류에 넣어 주세요.");
    }
  }

  private static void requireOpenSubcategory(Category top, Category sub) {
    requireSubcategory(top, sub);
    if (!top.isActive() || !sub.isActive()) {
      throw new DomainException("CATEGORY_HIDDEN", "숨긴 카테고리에는 질문을 넣을 수 없어요.");
    }
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
