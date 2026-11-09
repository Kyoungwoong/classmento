package com.classmento.questionbank.infrastructure;

import com.classmento.embedding.PgVectors;
import com.classmento.questionbank.domain.Category;
import com.classmento.questionbank.domain.JobRole;
import com.classmento.questionbank.domain.Level;
import com.classmento.questionbank.domain.QuestionCandidateRepository;
import com.classmento.questionbank.domain.QuestionPicker.Candidate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** {@link QuestionCandidateRepository}의 JDBC 구현. 숨긴 질문·숨긴 카테고리는 빼고, 태그·소분류 이름을 소문자 키로 붙인다. */
@Repository
public class JdbcQuestionCandidateRepository implements QuestionCandidateRepository {

  private final JdbcTemplate jdbc;

  public JdbcQuestionCandidateRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static final String BASE =
      """
      SELECT q.id, q.text, q.intent, q.level, c.id AS sub_id, c.name AS sub_name,
             ARRAY(SELECT t.name FROM qb_question_tag qt JOIN qb_tag t ON t.id = qt.tag_id
                   WHERE qt.question_id = q.id) AS tag_names
      FROM qb_question q
      JOIN qb_category c ON c.id = q.category_id
      JOIN qb_category p ON p.id = c.parent_id
      WHERE q.active AND c.active AND p.active
      """;

  private static final RowMapper<Candidate> MAPPER =
      (rs, i) -> {
        Set<String> keys = new HashSet<>();
        keys.add(rs.getString("sub_name").toLowerCase(Locale.ROOT));
        Arrays.stream((String[]) rs.getArray("tag_names").getArray())
            .map(n -> n.toLowerCase(Locale.ROOT))
            .forEach(keys::add);
        return new Candidate(
            rs.getLong("id"),
            rs.getString("text"),
            rs.getString("intent"),
            Level.valueOf(rs.getString("level")),
            rs.getLong("sub_id"),
            keys);
      };

  @Override
  public List<Candidate> nearestTech(float[] query, int k) {
    return jdbc.query(
        BASE
            + " AND p.name <> ? AND q.embedding IS NOT NULL"
            + " ORDER BY q.embedding <=> ?::vector LIMIT ?",
        MAPPER,
        Category.BEHAVIORAL_TOP,
        PgVectors.toLiteral(query),
        k);
  }

  @Override
  public List<Candidate> candidates(boolean behavioral, JobRole jobRole) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder(BASE);
    sql.append(behavioral ? " AND p.name = ?" : " AND p.name <> ?");
    args.add(Category.BEHAVIORAL_TOP);
    if (jobRole != null) {
      sql.append(
          " AND EXISTS (SELECT 1 FROM qb_question_job_role r"
              + " WHERE r.question_id = q.id AND r.job_role IN (?, 'COMMON'))");
      args.add(jobRole.name());
    }
    return jdbc.query(sql.toString(), MAPPER, args.toArray());
  }
}
