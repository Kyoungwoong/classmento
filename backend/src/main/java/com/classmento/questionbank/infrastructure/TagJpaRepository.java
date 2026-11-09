package com.classmento.questionbank.infrastructure;

import com.classmento.questionbank.domain.Tag;
import com.classmento.questionbank.domain.TagRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 도메인 포트 {@link TagRepository}의 Spring Data JPA 구현. */
public interface TagJpaRepository extends JpaRepository<Tag, Long>, TagRepository {

  @Override
  @Query(
      value =
          """
          SELECT * FROM qb_tag
          WHERE name_key LIKE '%' || :q || '%' ESCAPE '\\'
          ORDER BY (name_key LIKE :q || '%' ESCAPE '\\') DESC, name_key
          LIMIT :limit
          """,
      nativeQuery = true)
  List<Tag> search(@Param("q") String likeEscapedKey, @Param("limit") int limit);
}
