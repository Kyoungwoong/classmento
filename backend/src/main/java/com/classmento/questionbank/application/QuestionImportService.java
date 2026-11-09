package com.classmento.questionbank.application;

import com.classmento.common.web.ApiException;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.questionbank.domain.Category;
import com.classmento.questionbank.domain.CategoryRepository;
import com.classmento.questionbank.domain.Question;
import com.classmento.questionbank.domain.QuestionCsv;
import com.classmento.questionbank.domain.QuestionCsv.LineError;
import com.classmento.questionbank.domain.QuestionCsv.Parsed;
import com.classmento.questionbank.domain.QuestionCsv.Row;
import com.classmento.questionbank.domain.QuestionEmbeddingRepository;
import com.classmento.questionbank.domain.QuestionEmbeddingText;
import com.classmento.questionbank.domain.QuestionRepository;
import com.classmento.questionbank.domain.Tag;
import com.classmento.questionbank.domain.TagRepository;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CSV 대량 등록. 형식 검사(QuestionCsv) → 카테고리 확인 → 오류가 하나라도 있으면 0건. 모두 통과하면 임베딩을 트랜잭션 밖에서 한 번에 만들고, 태그
 * 찾기·만들기와 질문 저장은 한 트랜잭션에서 한다(전부 아니면 0건). 임베딩을 못 만들면 비워 두고 기동 시 채운다.
 */
@Service
public class QuestionImportService {

  public static final int MAX_BYTES = 2 * 1024 * 1024;
  private static final int EMBED_BATCH = 100;
  private static final Logger log = LoggerFactory.getLogger(QuestionImportService.class);

  private final CategoryRepository categories;
  private final TagRepository tags;
  private final QuestionRepository questions;
  private final QuestionEmbeddingRepository embeddings;
  private final EmbeddingClient embeddingClient;
  private final TransactionTemplate tx;

  public QuestionImportService(
      CategoryRepository categories,
      TagRepository tags,
      QuestionRepository questions,
      QuestionEmbeddingRepository embeddings,
      EmbeddingClient embeddingClient,
      TransactionTemplate tx) {
    this.categories = categories;
    this.tags = tags;
    this.questions = questions;
    this.embeddings = embeddings;
    this.embeddingClient = embeddingClient;
    this.tx = tx;
  }

  public Result importCsv(byte[] bytes, Long createdBy) {
    if (bytes.length > MAX_BYTES) {
      throw ApiException.badRequest("CSV 파일은 2MB까지 올릴 수 있어요.");
    }
    Parsed parsed = QuestionCsv.parse(utf8(bytes));
    if (!parsed.ok()) {
      return new Result(0, parsed.errors());
    }

    List<Category> all = categories.findAll();
    List<Placed> placed = new ArrayList<>();
    List<LineError> errors = new ArrayList<>();
    for (Row row : parsed.rows()) {
      String where = row.top() + " > " + row.sub();
      Category top = find(all, null, row.top());
      Category sub = top == null ? null : find(all, top.getId(), row.sub());
      if (sub == null) {
        errors.add(new LineError(row.line(), "없는 카테고리예요: " + where));
      } else if (!top.isActive() || !sub.isActive()) {
        errors.add(new LineError(row.line(), "숨긴 카테고리에는 넣을 수 없어요: " + where));
      } else {
        placed.add(new Placed(row, top, sub));
      }
    }
    if (!errors.isEmpty()) {
      return new Result(0, errors);
    }

    List<float[]> vectors = prepareEmbeddings(placed);
    return tx.execute(
        s -> {
          Map<String, Tag> tagByKey = new HashMap<>();
          for (int i = 0; i < placed.size(); i++) {
            Placed p = placed.get(i);
            Set<Long> tagIds = new LinkedHashSet<>();
            for (String name : p.row().tags()) {
              tagIds.add(
                  tagByKey.computeIfAbsent(Tag.keyOf(name), k -> findOrCreate(name)).getId());
            }
            Row r = p.row();
            Question q =
                questions.save(
                    Question.create(
                        p.top(),
                        p.sub(),
                        r.text(),
                        r.level(),
                        r.jobRoles(),
                        tagIds,
                        r.intent(),
                        createdBy));
            if (vectors != null) {
              embeddings.update(q.getId(), vectors.get(i));
            }
          }
          return new Result(placed.size(), List.of());
        });
  }

  private Tag findOrCreate(String name) {
    return tags.findByKey(Tag.keyOf(name)).orElseGet(() -> tags.save(Tag.of(name)));
  }

  /** 이미 있는 태그는 저장된 이름으로 문장을 만든다(대소문자 차이). 실패하면 null → 기동 시 채우기. */
  private List<float[]> prepareEmbeddings(List<Placed> placed) {
    if (!embeddingClient.isAvailable()) {
      return null;
    }
    Map<String, String> nameByKey = new HashMap<>();
    List<String> texts =
        placed.stream()
            .map(
                p ->
                    QuestionEmbeddingText.of(
                        p.top().getName(),
                        p.sub().getName(),
                        p.row().text(),
                        p.row().tags().stream()
                            .map(
                                n ->
                                    nameByKey.computeIfAbsent(
                                        Tag.keyOf(n),
                                        k -> tags.findByKey(k).map(Tag::getName).orElse(n)))
                            .toList()))
            .toList();
    try {
      // 한 요청이 너무 커지지 않게(토큰 한도·시간) 나눠 부른다
      List<float[]> vectors = new ArrayList<>(texts.size());
      for (int from = 0; from < texts.size(); from += EMBED_BATCH) {
        vectors.addAll(
            embeddingClient.embedAll(
                texts.subList(from, Math.min(from + EMBED_BATCH, texts.size()))));
      }
      return vectors;
    } catch (RuntimeException e) {
      log.warn(
          "CSV 질문 임베딩 생성 실패(비워 두고 나중에 채움): {}: {}", e.getClass().getSimpleName(), e.getMessage());
      return null;
    }
  }

  /** parentId가 null이면 대분류. 이름은 공백·대소문자를 무시하고 비교한다. */
  private static Category find(List<Category> all, Long parentId, String name) {
    String key = Category.nameKey(name);
    return all.stream()
        .filter(c -> Objects.equals(c.getParentId(), parentId))
        .filter(c -> Category.nameKey(c.getName()).equals(key))
        .findFirst()
        .orElse(null);
  }

  private static String utf8(byte[] bytes) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException e) {
      throw ApiException.badRequest("UTF-8로 저장한 CSV를 올려 주세요. 엑셀이라면 'CSV UTF-8'로 저장해요.");
    }
  }

  private record Placed(Row row, Category top, Category sub) {}

  /** imported: 저장한 질문 수(오류가 있으면 0). errors: 줄별 오류. */
  public record Result(int imported, List<LineError> errors) {}
}
