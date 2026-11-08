package com.classmento.resume.domain;

import com.classmento.common.domain.DomainException;
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

  /** UPLOADED → EXTRACTED: 텍스트 추출과 청크 임베딩이 끝났다. */
  public void markExtracted(String text) {
    requireStatus(Status.UPLOADED);
    this.extractedText = text;
    this.status = Status.EXTRACTED;
  }

  /** EXTRACTED → PARSED: 구조화 결과를 저장한다. 어떤 모델·프롬프트 버전으로 만들었는지 함께 남긴다. */
  public void markParsed(String json, String model, String promptVersion) {
    requireStatus(Status.EXTRACTED);
    this.parsedJson = json;
    this.parseModel = model;
    this.promptVersion = promptVersion;
    this.status = Status.PARSED;
    this.failureReason = null;
  }

  /** 사용자가 구조화 결과를 고친다. 파싱이 끝난 뒤에만 가능하다. */
  public void updateParsed(String json) {
    if (status != Status.PARSED) {
      throw new DomainException("RESUME_NOT_READY", "아직 이력서를 읽고 있어요. 잠시 후 다시 시도해 주세요.");
    }
    this.parsedJson = json;
  }

  /** 처리 중(UPLOADED/EXTRACTED) 실패. 사유는 사용자에게 보여줄 문장. */
  public void markFailed(String reason) {
    requireStatus(Status.UPLOADED, Status.EXTRACTED);
    this.status = Status.FAILED;
    this.failureReason = reason;
  }

  private void requireStatus(Status... allowed) {
    for (Status s : allowed) {
      if (status == s) {
        return;
      }
    }
    // 메시지는 사용자용 문장이라 내부 상태 이름(enum)은 넣지 않는다
    throw new DomainException("RESUME_INVALID_STATE", "이력서 처리 상태가 맞지 않아요. 다시 시도해 주세요.");
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
