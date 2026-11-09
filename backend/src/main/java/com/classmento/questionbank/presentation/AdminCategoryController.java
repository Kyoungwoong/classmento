package com.classmento.questionbank.presentation;

import com.classmento.questionbank.application.CategoryService;
import com.classmento.questionbank.presentation.QuestionBankDtos.CategoryTreeNode;
import com.classmento.questionbank.presentation.QuestionBankDtos.CategoryView;
import com.classmento.questionbank.presentation.QuestionBankDtos.CreateCategoryRequest;
import com.classmento.questionbank.presentation.QuestionBankDtos.UpdateCategoryRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 질문 은행 카테고리 관리(ROLE_ADMIN, /api/admin/**). */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

  private final CategoryService service;

  public AdminCategoryController(CategoryService service) {
    this.service = service;
  }

  @GetMapping
  public List<CategoryTreeNode> tree() {
    return service.tree().stream().map(CategoryTreeNode::of).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CategoryView create(@Valid @RequestBody CreateCategoryRequest body) {
    return CategoryView.of(service.create(body.parentId(), body.name()));
  }

  @PatchMapping("/{id}")
  public CategoryView update(
      @PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest body) {
    return CategoryView.of(service.update(id, body.name(), body.sortOrder(), body.active()));
  }
}
