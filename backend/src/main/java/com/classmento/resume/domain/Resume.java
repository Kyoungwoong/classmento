package com.classmento.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resume")
public class Resume {

  public enum Status {
    /** 파일 저장 완료, 처리 대기 */
    UPLOADED,
    /** 텍스트 추출 + 청크 임베딩 완료, 구조화 중 */
    EXTRACTED,
    /** 구조화 완료 — 사용자 확인 가능 */
    PARSED,
    FAILED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private String originalFilename;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private long sizeBytes;

  @Column(nullable = false)
  private String storageKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.UPLOADED;

  private String failureReason;

  @Column(columnDefinition = "text")
  private String extractedText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String parsedJson;

  private String parseModel;

  private String promptVersion;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  protected Resume() {}

  public Resume(
      Long userId, String originalFilename, String contentType, long sizeBytes, String storageKey) {
    this.userId = userId;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.storageKey = storageKey;
  }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public void markExtracted(String text) {
    this.extractedText = text;
    this.status = Status.EXTRACTED;
  }

  public void markParsed(String json, String model, String promptVersion) {
    this.parsedJson = json;
    this.parseModel = model;
    this.promptVersion = promptVersion;
    this.status = Status.PARSED;
    this.failureReason = null;
  }

  public void updateParsed(String json) {
    this.parsedJson = json;
  }

  public void markFailed(String reason) {
    this.status = Status.FAILED;
    this.failureReason = reason;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public Status getStatus() {
    return status;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public String getExtractedText() {
    return extractedText;
  }

  public String getParsedJson() {
    return parsedJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
