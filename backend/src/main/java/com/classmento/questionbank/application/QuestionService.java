package com.classmento.questionbank.application;

import com.classmento.common.web.ApiException;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.questionbank.domain.Category;
import com.classmento.questionbank.domain.CategoryRepository;
import com.classmento.questionbank.domain.JobRole;
import com.classmento.questionbank.domain.Level;
import com.classmento.questionbank.domain.Question;
import com.classmento.questionbank.domain.QuestionEmbeddingRepository;
import com.classmento.questionbank.domain.QuestionEmbeddingText;
import com.classmento.questionbank.domain.QuestionRepository;
import com.classmento.questionbank.domain.QuestionSearchRepository;
import com.classmento.questionbank.domain.QuestionSearchRepository.Filter;
import com.classmento.questionbank.domain.QuestionSearchRepository.IdPage;
import com.classmento.questionbank.domain.Tag;
import com.classmento.questionbank.domain.TagRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 질문 관리 유스케이스(관리자). 임베딩은 외부 API라 트랜잭션 밖에서 만들고(prepare), 질문 저장과 임베딩 기록은 한 트랜잭션에서 한다. 임베딩을 쓸 수 없으면(키
 * 없음) 비워 두고 기동 시 채운다({@link QuestionEmbeddingBackfill}).
 */
@Service
public class QuestionService {

  private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

  private final QuestionRepository questions;
  private final CategoryRepository categories;
  private final TagRepository tags;
  private final QuestionSearchRepository search;
  private final QuestionEmbeddingRepository embeddings;
  private final EmbeddingClient embeddingClient;
  private final TransactionTemplate tx;

  public QuestionService(
      QuestionRepository questions,
      CategoryRepository categories,
      TagRepository tags,
      QuestionSearchRepository search,
      QuestionEmbeddingRepository embeddings,
      EmbeddingClient embeddingClient,
      TransactionTemplate tx) {
    this.questions = questions;
    this.categories = categories;
    this.tags = tags;
    this.search = search;
    this.embeddings = embeddings;
    this.embeddingClient = embeddingClient;
    this.tx = tx;
  }

  public Details create(Command c, Long createdBy) {
    float[] vector = prepareEmbedding(c);
    return tx.execute(
        s -> {
          Refs r = refs(c);
          Question q =
              questions.save(
                  Question.create(
                      r.top(),
                      r.sub(),
                      c.text(),
                      c.level(),
                      c.jobRoles(),
                      c.tagIds(),
                      c.intent(),
                      createdBy));
          writeEmbedding(q.getId(), vector);
          return new Details(q, r.top(), r.sub(), r.tags());
        });
  }

  public Details update(Long id, Command c) {
    questions.findById(id).orElseThrow(ApiException::notFound);
    float[] vector = prepareEmbedding(c);
    return tx.execute(
        s -> {
          Question q = questions.lockById(id).orElseThrow(ApiException::notFound);
          Refs r = refs(c);
          q.edit(r.top(), r.sub(), c.text(), c.level(), c.jobRoles(), c.tagIds(), c.intent());
          writeEmbedding(q.getId(), vector);
          return new Details(q, r.top(), r.sub(), r.tags());
        });
  }

  public Details setActive(Long id, boolean active) {
    return tx.execute(
        s -> {
          Question q = questions.lockById(id).orElseThrow(ApiException::notFound);
          if (active) {
            q.show();
          } else {
            q.hide();
          }
          return detailsOf(List.of(q)).get(0);
        });
  }

  public Page search(Filter filter, int page, int size) {
    return tx.execute(
        s -> {
          IdPage ids = search.search(filter, page, size);
          Map<Long, Question> byId =
              questions.findAllById(ids.ids()).stream()
                  .collect(Collectors.toMap(Question::getId, Function.identity()));
          List<Question> ordered = ids.ids().stream().map(byId::get).toList();
          return new Page(detailsOf(ordered), ids.total(), page, size);
        });
  }

  private float[] prepareEmbedding(Command c) {
    Refs r = refs(c);
    if (!embeddingClient.isAvailable()) {
      return null;
    }
    String text =
        QuestionEmbeddingText.of(
            r.top().getName(),
            r.sub().getName(),
            c.text() == null ? "" : c.text().strip(),
            r.tags().stream().map(Tag::getName).toList());
    try {
      return embeddingClient.embed(text);
    } catch (RuntimeException e) {
      // 외부 장애로 질문 저장까지 막지 않는다. 임베딩은 비워 두고 기동 시 채운다.
      log.warn("질문 임베딩 생성 실패(비워 두고 나중에 채움): {}: {}", e.getClass().getSimpleName(), e.getMessage());
      return null;
    }
  }

  private void writeEmbedding(Long questionId, float[] vector) {
    if (vector != null) {
      embeddings.update(questionId, vector);
    } else {
      // 예전 내용의 벡터가 남지 않게 비운다(QuestionEmbeddingBackfill이 다시 채운다).
      embeddings.clear(questionId);
    }
  }

  /** 카테고리(없으면 404)와 태그(없는 게 섞이면 400)를 불러온다. 대분류를 고르면 top=sub로 넘겨 도메인이 거절한다. */
  private Refs refs(Command c) {
    Category sub = categories.findById(c.categoryId()).orElseThrow(ApiException::notFound);
    Category top =
        sub.isTop()
            ? sub
            : categories.findById(sub.getParentId()).orElseThrow(ApiException::notFound);
    Set<Long> tagIds = c.tagIds() == null ? Set.of() : c.tagIds();
    List<Tag> found = tagIds.isEmpty() ? List.of() : tags.findAllById(tagIds);
    if (found.size() != tagIds.size()) {
      throw ApiException.badRequest("없는 태그가 있어요. 태그를 다시 골라 주세요.");
    }
    return new Refs(top, sub, sortByName(found));
  }

  private List<Details> detailsOf(List<Question> list) {
    Map<Long, Category> cats =
        categories.findAll().stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));
    Set<Long> tagIds = new HashSet<>();
    list.forEach(q -> tagIds.addAll(q.getTagIds()));
    Map<Long, Tag> tagById =
        tagIds.isEmpty()
            ? Map.of()
            : tags.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
    return list.stream()
        .map(
            q -> {
              Category sub = cats.get(q.getCategoryId());
              return new Details(
                  q,
                  cats.get(sub.getParentId()),
                  sub,
                  sortByName(q.getTagIds().stream().map(tagById::get).toList()));
            })
        .toList();
  }

  private static List<Tag> sortByName(List<Tag> list) {
    return list.stream().sorted(Comparator.comparing(Tag::getName)).toList();
  }

  public record Command(
      Long categoryId,
      String text,
      Level level,
      Set<JobRole> jobRoles,
      Set<Long> tagIds,
      String intent) {}

  /** 질문과 화면에 보일 이름들(대분류·소분류·태그). */
  public record Details(Question question, Category top, Category sub, List<Tag> tags) {}

  public record Page(List<Details> items, long total, int page, int size) {}

  private record Refs(Category top, Category sub, List<Tag> tags) {}
}
