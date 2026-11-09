package com.classmento.questionbank.presentation;

import com.classmento.common.security.AuthUser;
import com.classmento.common.web.ApiException;
import com.classmento.questionbank.application.QuestionImportService;
import com.classmento.questionbank.application.QuestionService;
import com.classmento.questionbank.domain.JobRole;
import com.classmento.questionbank.domain.Level;
import com.classmento.questionbank.domain.QuestionSearchRepository.Filter;
import com.classmento.questionbank.presentation.QuestionBankDtos.ImportResult;
import com.classmento.questionbank.presentation.QuestionBankDtos.QuestionPage;
import com.classmento.questionbank.presentation.QuestionBankDtos.QuestionRequest;
import com.classmento.questionbank.presentation.QuestionBankDtos.QuestionView;
import com.classmento.questionbank.presentation.QuestionBankDtos.SetActiveRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 질문 은행 질문 관리(ROLE_ADMIN, /api/admin/**). */
@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

  private static final int MAX_SIZE = 100;

  private final QuestionService service;
  private final QuestionImportService importService;

  public AdminQuestionController(QuestionService service, QuestionImportService importService) {
    this.service = service;
    this.importService = importService;
  }

  /** 최신순. 조건은 모두 선택이고 함께 쓰면 AND. 숨긴 질문은 includeHidden=true일 때만. */
  @GetMapping
  @Operation(operationId = "searchAdminQuestions")
  public QuestionPage search(
      @RequestParam(required = false) Long topCategoryId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Level level,
      @RequestParam(required = false) JobRole jobRole,
      @RequestParam(required = false) Long tagId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "false") boolean includeHidden,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Filter filter = new Filter(topCategoryId, categoryId, level, jobRole, tagId, q, includeHidden);
    return QuestionPage.of(
        service.search(filter, Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE)));
  }

  /** CSV 대량 등록(UTF-8, 2MB, 1000줄). 형식: docs/api/README.md */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(operationId = "importAdminQuestions")
  public ImportResult importCsv(
      @RequestPart("file") MultipartFile file, @AuthenticationPrincipal AuthUser user)
      throws IOException {
    if (file.getSize() > QuestionImportService.MAX_BYTES) {
      throw ApiException.badRequest("CSV 파일은 2MB까지 올릴 수 있어요.");
    }
    return ImportResult.of(importService.importCsv(file.getBytes(), user.id()));
  }

  @GetMapping("/{id}")
  @Operation(operationId = "getAdminQuestion")
  public QuestionView get(@PathVariable Long id) {
    return QuestionView.of(service.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public QuestionView create(
      @Valid @RequestBody QuestionRequest body, @AuthenticationPrincipal AuthUser user) {
    return QuestionView.of(service.create(body.toCommand(), user.id()));
  }

  @PutMapping("/{id}")
  public QuestionView update(@PathVariable Long id, @Valid @RequestBody QuestionRequest body) {
    return QuestionView.of(service.update(id, body.toCommand()));
  }

  @PatchMapping("/{id}/active")
  public QuestionView setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest body) {
    return QuestionView.of(service.setActive(id, body.active()));
  }
}
