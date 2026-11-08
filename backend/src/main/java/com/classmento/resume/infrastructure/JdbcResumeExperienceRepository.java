package com.classmento.resume.infrastructure;

import com.classmento.embedding.PgVectors;
import com.classmento.resume.domain.ResumeExperience;
import com.classmento.resume.domain.ResumeExperienceRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** {@link ResumeExperienceRepository}의 JDBC 구현. 기술 목록은 jsonb, 임베딩은 pgvector. */
@Repository
public class JdbcResumeExperienceRepository implements ResumeExperienceRepository {

  private static final String COLUMNS =
      "id, seq, type, title, organization, period, summary, detail, skills";

  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public JdbcResumeExperienceRepository(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  @Override
  @Transactional
  public void replaceAll(
      Long resumeId, List<ResumeExperience> experiences, List<float[]> embeddings) {
    if (experiences.size() != embeddings.size()) {
      throw new IllegalArgumentException("경험과 임베딩 개수가 다릅니다.");
    }
    jdbc.update("DELETE FROM resume_experience WHERE resume_id = ?", resumeId);
    for (int i = 0; i < experiences.size(); i++) {
      ResumeExperience e = experiences.get(i);
      jdbc.update(
          """
          INSERT INTO resume_experience
            (resume_id, seq, type, title, organization, period, summary, detail, skills, embedding)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::vector)
          """,
          resumeId,
          e.seq(),
          e.type().name(),
          e.title(),
          e.organization(),
          e.period(),
          e.summary(),
          e.detail(),
          json.writeValueAsString(e.skills()),
          PgVectors.toLiteral(embeddings.get(i)));
    }
  }

  @Override
  public List<ResumeExperience> findByResumeId(Long resumeId) {
    return jdbc.query(
        "SELECT " + COLUMNS + " FROM resume_experience WHERE resume_id = ? ORDER BY seq",
        mapper(),
        resumeId);
  }

  @Override
  public List<ResumeExperience> findNearest(Long resumeId, float[] query, int k) {
    return jdbc.query(
        "SELECT "
            + COLUMNS
            + " FROM resume_experience WHERE resume_id = ? ORDER BY embedding <=> ?::vector LIMIT ?",
        mapper(),
        resumeId,
        PgVectors.toLiteral(query),
        k);
  }

  private RowMapper<ResumeExperience> mapper() {
    return (ResultSet rs, int i) -> toExperience(rs);
  }

  private ResumeExperience toExperience(ResultSet rs) throws SQLException {
    List<String> skills = json.readValue(rs.getString("skills"), new TypeReference<>() {});
    return new ResumeExperience(
        rs.getLong("id"),
        rs.getInt("seq"),
        ResumeExperience.Type.valueOf(rs.getString("type")),
        rs.getString("title"),
        rs.getString("organization"),
        rs.getString("period"),
        rs.getString("summary"),
        rs.getString("detail"),
        skills);
  }
}
