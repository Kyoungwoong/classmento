package com.classmento.questionbank.infrastructure;

import com.classmento.embedding.PgVectors;
import com.classmento.questionbank.domain.QuestionEmbeddingRepository;
import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@link QuestionEmbeddingRepository}의 JDBC 구현. 질문 은행 전체를 검색하므로 HNSW 인덱스(vector_cosine_ops)를 탄다.
 * {@code <=>}는 코사인 거리.
 */
@Repository
public class JdbcQuestionEmbeddingRepository implements QuestionEmbeddingRepository {

  private final JdbcTemplate jdbc;

  public JdbcQuestionEmbeddingRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void update(Long questionId, float[] embedding) {
    jdbc.update(
        "UPDATE qb_question SET embedding = ?::vector WHERE id = ?",
        PgVectors.toLiteral(embedding),
        questionId);
  }

  @Override
  public List<Missing> findMissing(int limit) {
    return jdbc.query(
        """
        SELECT q.id, p.name AS top_name, c.name AS sub_name, q.text,
               ARRAY(SELECT t.name FROM qb_question_tag qt JOIN qb_tag t ON t.id = qt.tag_id
                     WHERE qt.question_id = q.id) AS tag_names
        FROM qb_question q
        JOIN qb_category c ON c.id = q.category_id
        JOIN qb_category p ON p.id = c.parent_id
        WHERE q.embedding IS NULL
        ORDER BY q.id
        LIMIT ?
        """,
        (rs, i) -> {
          Array tags = rs.getArray("tag_names");
          return new Missing(
              rs.getLong("id"),
              rs.getString("top_name"),
              rs.getString("sub_name"),
              rs.getString("text"),
              Arrays.asList((String[]) tags.getArray()));
        },
        limit);
  }

  @Override
  public List<Match> findNearest(float[] query, int k) {
    return jdbc.query(
        """
        SELECT id, text, embedding <=> ?::vector AS distance
        FROM qb_question
        WHERE active AND embedding IS NOT NULL
        ORDER BY distance
        LIMIT ?
        """,
        (rs, i) -> new Match(rs.getLong("id"), rs.getString("text"), rs.getDouble("distance")),
        PgVectors.toLiteral(query),
        k);
  }
}
