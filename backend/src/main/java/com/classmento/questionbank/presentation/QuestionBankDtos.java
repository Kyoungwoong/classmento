package com.classmento.questionbank.presentation;

import com.classmento.questionbank.application.CategoryService.CategoryNode;
import com.classmento.questionbank.domain.Category;
import com.classmento.questionbank.domain.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

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

  @Schema(requiredProperties = {"id", "name", "sortOrder", "active", "children"})
  public record CategoryTreeNode(
      Long id, String name, int sortOrder, boolean active, List<CategoryView> children) {

    static CategoryTreeNode of(CategoryNode node) {
      Category top = node.top();
      return new CategoryTreeNode(
          top.getId(),
          top.getName(),
          top.getSortOrder(),
          top.isActive(),
          node.children().stream().map(CategoryView::of).toList());
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
