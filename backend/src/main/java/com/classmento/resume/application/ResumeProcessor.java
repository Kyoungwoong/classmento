package com.classmento.resume.application;

import com.classmento.common.domain.DomainException;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeChunkRepository;
import com.classmento.resume.domain.ResumeFileType;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.resume.domain.TextChunker;
import com.classmento.storage.FileStorage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 업로드 이후의 무거운 처리. 각 단계가 끝날 때마다 상태를 저장해 화면이 진행 상황을 폴링할 수 있게 한다.
 *
 * <pre>
 * UPLOADED → (텍스트 추출, 청크, 임베딩) → EXTRACTED → (Claude 구조화 → 경험 분리·임베딩) → PARSED
 *                                  실패 시 어느 단계든 → FAILED + 사용자용 사유
 * </pre>
 */
@Component
public class ResumeProcessor {

  private static final Logger log = LoggerFactory.getLogger(ResumeProcessor.class);

  private final ResumeRepository resumes;
  private final ResumeChunkRepository chunks;
  private final FileStorage storage;
  private final TextExtractor extractor;
  private final EmbeddingClient embeddings;
  private final ResumeParser parser;
  private final ExperienceSync experienceSync;
  private final TransactionTemplate tx;
  private final TextChunker chunker = new TextChunker(800, 100);

  public ResumeProcessor(
      ResumeRepository resumes,
      ResumeChunkRepository chunks,
      FileStorage storage,
      TextExtractor extractor,
      EmbeddingClient embeddings,
      ResumeParser parser,
      ExperienceSync experienceSync,
      TransactionTemplate tx) {
    this.resumes = resumes;
    this.chunks = chunks;
    this.storage = storage;
    this.extractor = extractor;
    this.embeddings = embeddings;
    this.parser = parser;
    this.experienceSync = experienceSync;
    this.tx = tx;
  }

  public void process(Long resumeId) {
    Resume resume = resumes.findById(resumeId).orElseThrow();
    try {
      // 1) 텍스트 추출
      byte[] file = storage.load(resume.getStorageKey());
      String text =
          extractor.extract(ResumeFileType.fromContentType(resume.getContentType()), file);
      if (text.length() < 50) {
        fail(resumeId, "글자를 읽을 수 없는 파일이에요. 스캔한 이미지 PDF는 아직 지원하지 않아요.");
        return;
      }

      // 2) 청크 + 임베딩 (RAG 검색용)
      List<String> pieces = chunker.chunk(text);
      List<float[]> vectors = embeddings.embedAll(pieces);
      chunks.replaceAll(resumeId, pieces, vectors);
      update(resumeId, r -> r.markExtracted(text));

      // 3) 구조화
      ResumeParser.Result result = parser.parse(text);
      // 4) 경험 단위 분리 + 임베딩 (경험 기반 면접의 재료, ADR 0005).
      //    임베딩은 트랜잭션 밖, 경험 저장과 PARSED 전이는 한 트랜잭션(둘 중 하나만 남지 않게)
      ExperienceSync.Prepared experiences = experienceSync.prepare(result.parsed());
      update(
          resumeId,
          r -> {
            experienceSync.save(resumeId, experiences);
            r.markParsed(result.json(), result.model(), result.promptVersion());
          });
    } catch (Exception e) {
      log.warn("이력서 처리 실패 id={}", resumeId, e);
      fail(resumeId, "이력서를 읽는 중에 문제가 생겼어요. 다시 올려주세요.");
    }
  }

  /**
   * 처리 실패 기록. 이미 PARSED/FAILED인 이력서는 도메인 규칙상 실패로 바꿀 수 없으므로(재처리 중복 등) 예외를 실행기 밖으로 흘리지 않고 로그만 남긴다.
   * 재처리(FAILED → UPLOADED) 전이는 재시도 기능을 만들 때 정한다.
   */
  private void fail(Long resumeId, String reason) {
    try {
      update(resumeId, r -> r.markFailed(reason));
    } catch (DomainException e) {
      log.warn("이력서 실패 기록 생략 id={} ({})", resumeId, e.code());
    }
  }

  private void update(Long resumeId, java.util.function.Consumer<Resume> change) {
    tx.executeWithoutResult(
        status -> {
          Resume r = resumes.findById(resumeId).orElseThrow();
          change.accept(r);
        });
  }
}
