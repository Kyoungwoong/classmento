package com.classmento.resume;

import com.classmento.embedding.PgVectors;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이력서 청크 + 임베딩 저장/검색. pgvector 연산자를 직접 쓰기 위해 JPA 대신 JdbcTemplate을 쓴다.
 *
 * <ul>
 *   <li>{@code <=>} 코사인 거리(0 = 같은 방향, 2 = 반대). 인덱스: HNSW vector_cosine_ops
 * </ul>
 */
@Repository
public class ResumeChunkRepository {

  private final JdbcTemplate jdbc;

  public ResumeChunkRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** 기존 청크를 지우고 새로 저장한다(재처리 시 중복 방지). */
  @Transactional
  public void replaceAll(Long resumeId, List<String> contents, List<float[]> embeddings) {
    jdbc.update("DELETE FROM resume_chunk WHERE resume_id = ?", resumeId);
    for (int i = 0; i < contents.size(); i++) {
      jdbc.update(
          "INSERT INTO resume_chunk (resume_id, seq, content, embedding) VALUES (?, ?, ?, ?::vector)",
          resumeId,
          i,
          contents.get(i),
          PgVectors.toLiteral(embeddings.get(i)));
    }
  }

  /** 질문(쿼리) 벡터와 가장 가까운 청크 k개. 다음 단계(질문 생성 RAG)에서 사용한다. */
  public List<ChunkMatch> findNearest(Long resumeId, float[] query, int k) {
    return jdbc.query(
        """
        SELECT seq, content, embedding <=> ?::vector AS distance
        FROM resume_chunk
        WHERE resume_id = ?
        ORDER BY distance
        LIMIT ?
        """,
        (rs, i) ->
            new ChunkMatch(rs.getInt("seq"), rs.getString("content"), rs.getDouble("distance")),
        PgVectors.toLiteral(query),
        resumeId,
        k);
  }

  public int count(Long resumeId) {
    Integer n =
        jdbc.queryForObject(
            "SELECT count(*) FROM resume_chunk WHERE resume_id = ?", Integer.class, resumeId);
    return n == null ? 0 : n;
  }

  public record ChunkMatch(int seq, String content, double distance) {}
}
