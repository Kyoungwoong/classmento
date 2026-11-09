package com.classmento.questionbank.infrastructure;

import com.classmento.questionbank.domain.QuestionSearchRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** {@link QuestionSearchRepository}의 JDBC 구현. 조건을 모아 WHERE를 만든다(값은 모두 바인딩). */
@Repository
public class JdbcQuestionSearchRepository implements QuestionSearchRepository {

  private final JdbcTemplate jdbc;

  public JdbcQuestionSearchRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public IdPage search(Filter f, int page, int size) {
    List<String> where = new ArrayList<>();
    List<Object> args = new ArrayList<>();
    if (!f.includeHidden()) {
      where.add("q.active");
    }
    if (f.topCategoryId() != null) {
      where.add("c.parent_id = ?");
      args.add(f.topCategoryId());
    }
    if (f.categoryId() != null) {
      where.add("q.category_id = ?");
      args.add(f.categoryId());
    }
    if (f.level() != null) {
      where.add("q.level = ?");
      args.add(f.level().name());
    }
    if (f.jobRole() != null) {
      where.add(
          "EXISTS (SELECT 1 FROM qb_question_job_role r"
              + " WHERE r.question_id = q.id AND r.job_role = ?)");
      args.add(f.jobRole().name());
    }
    if (f.tagId() != null) {
      where.add(
          "EXISTS (SELECT 1 FROM qb_question_tag t WHERE t.question_id = q.id AND t.tag_id = ?)");
      args.add(f.tagId());
    }
    if (f.query() != null && !f.query().isBlank()) {
      where.add("q.text ILIKE '%' || ? || '%' ESCAPE '\\'");
      args.add(escapeLike(f.query().strip()));
    }
    String from =
        " FROM qb_question q JOIN qb_category c ON c.id = q.category_id"
            + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where));

    Long total = jdbc.queryForObject("SELECT count(*)" + from, Long.class, args.toArray());
    List<Object> pageArgs = new ArrayList<>(args);
    pageArgs.add(size);
    pageArgs.add((long) page * size);
    List<Long> ids =
        jdbc.queryForList(
            "SELECT q.id" + from + " ORDER BY q.created_at DESC, q.id DESC LIMIT ? OFFSET ?",
            Long.class,
            pageArgs.toArray());
    return new IdPage(ids, total == null ? 0 : total);
  }

  @Override
  public Map<Long, Long> countByCategory() {
    Map<Long, Long> counts = new HashMap<>();
    jdbc.query(
        "SELECT category_id, count(*) AS n FROM qb_question GROUP BY category_id",
        rs -> {
          counts.put(rs.getLong("category_id"), rs.getLong("n"));
        });
    return counts;
  }

  private static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
