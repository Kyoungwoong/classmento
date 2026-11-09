package com.classmento.questionbank.presentation;

import com.classmento.questionbank.application.CategoryService.CategoryNode;
import com.classmento.questionbank.application.QuestionService.Command;
import com.classmento.questionbank.application.QuestionService.Details;
import com.classmento.questionbank.application.QuestionService.Page;
import com.classmento.questionbank.domain.Category;
import com.classmento.questionbank.domain.JobRole;
import com.classmento.questionbank.domain.Level;
import com.classmento.questionbank.domain.Question;
import com.classmento.questionbank.domain.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** 질문 은행 관리자 API 요청/응답. */
public final class QuestionBankDtos {

  private QuestionBankDtos() {}

  public record CreateCategoryRequest(
      Long parentId,
      @NotBlank(message = "카테고리 이름을 입력해 주세요.")
          @Size(max = Category.NAME_MAX, message = "카테고리 이름은 30자까지예요.")
          String name) {}

  /** 바꿀 항목만 보낸다(null은 그대로). */
  public record UpdateCategoryRequest(
      @Pattern(regexp = "(?s).*\\S.*", message = "카테고리 이름을 입력해 주세요.")
          @Size(max = Category.NAME_MAX, message = "카테고리 이름은 30자까지예요.")
          String name,
      Integer sortOrder,
      Boolean active) {}

  @Schema(requiredProperties = {"id", "name", "sortOrder", "active"})
  public record CategoryView(Long id, Long parentId, String name, int sortOrder, boolean active) {

    static CategoryView of(Category c) {
      return new CategoryView(
          c.getId(), c.getParentId(), c.getName(), c.getSortOrder(), c.isActive());
    }
  }

  /** 트리의 소분류. questionCount는 숨긴 질문도 센다. */
  @Schema(requiredProperties = {"id", "parentId", "name", "sortOrder", "active", "questionCount"})
  public record SubcategoryView(
      Long id, Long parentId, String name, int sortOrder, boolean active, long questionCount) {}

  @Schema(requiredProperties = {"id", "name", "sortOrder", "active", "children"})
  public record CategoryTreeNode(
      Long id, String name, int sortOrder, boolean active, List<SubcategoryView> children) {

    static CategoryTreeNode of(CategoryNode node) {
      Category top = node.top();
      return new CategoryTreeNode(
          top.getId(),
          top.getName(),
          top.getSortOrder(),
          top.isActive(),
          node.children().stream()
              .map(
                  c ->
                      new SubcategoryView(
                          c.getId(),
                          c.getParentId(),
                          c.getName(),
                          c.getSortOrder(),
                          c.isActive(),
                          node.questionCount(c)))
              .toList());
    }
  }

  public record QuestionRequest(
      @NotNull(message = "소분류를 골라 주세요.") Long categoryId,
      @NotBlank(message = "질문을 입력해 주세요.")
          @Size(min = Question.TEXT_MIN, max = Question.TEXT_MAX, message = "질문은 5~300자로 입력해 주세요.")
          String text,
      @NotNull(message = "난이도를 골라 주세요.") Level level,
      @NotEmpty(message = "직무를 하나 이상 골라 주세요.") Set<JobRole> jobRoles,
      @Size(max = Question.TAGS_MAX, message = "태그는 10개까지 붙일 수 있어요.") Set<Long> tagIds,
      @Size(max = Question.INTENT_MAX, message = "질문 의도는 200자까지예요.") String intent) {

    Command toCommand() {
      return new Command(
          categoryId, text, level, jobRoles, tagIds == null ? Set.of() : tagIds, intent);
    }
  }

  public record SetActiveRequest(@NotNull(message = "보일지 숨길지 골라 주세요.") Boolean active) {}

  @Schema(
      requiredProperties = {
        "id",
        "topCategoryId",
        "topCategoryName",
        "categoryId",
        "categoryName",
        "text",
        "level",
        "jobRoles",
        "tags",
        "active",
        "createdAt"
      })
  public record QuestionView(
      Long id,
      Long topCategoryId,
      String topCategoryName,
      Long categoryId,
      String categoryName,
      String text,
      Level level,
      List<JobRole> jobRoles,
      List<TagView> tags,
      String intent,
      boolean active,
      Instant createdAt) {

    static QuestionView of(Details d) {
      Question q = d.question();
      return new QuestionView(
          q.getId(),
          d.top().getId(),
          d.top().getName(),
          d.sub().getId(),
          d.sub().getName(),
          q.getText(),
          q.getLevel(),
          q.getJobRoles().stream().sorted().toList(),
          d.tags().stream().map(TagView::of).toList(),
          q.getIntent(),
          q.isActive(),
          q.getCreatedAt());
    }
  }

  @Schema(requiredProperties = {"items", "total", "page", "size"})
  public record QuestionPage(List<QuestionView> items, long total, int page, int size) {

    static QuestionPage of(Page p) {
      return new QuestionPage(
          p.items().stream().map(QuestionView::of).toList(), p.total(), p.page(), p.size());
    }
  }

  public record CreateTagRequest(
      @NotBlank(message = "태그를 입력해 주세요.")
          @Size(max = Tag.NAME_MAX, message = "태그는 20자까지예요.")
          @Pattern(regexp = "[^,]*", message = "태그에는 쉼표를 쓸 수 없어요.")
          String name) {}

  @Schema(requiredProperties = {"id", "name"})
  public record TagView(Long id, String name) {

    static TagView of(Tag t) {
      return new TagView(t.getId(), t.getName());
    }
  }

  /** created: 새로 만들었으면 true, 이미 있던 태그면 false. */
  @Schema(requiredProperties = {"tag", "created"})
  public record TagResult(TagView tag, boolean created) {}
}
