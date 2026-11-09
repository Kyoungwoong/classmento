package com.classmento.questionbank.presentation;

import com.classmento.questionbank.application.TagService;
import com.classmento.questionbank.presentation.QuestionBankDtos.CreateTagRequest;
import com.classmento.questionbank.presentation.QuestionBankDtos.TagResult;
import com.classmento.questionbank.presentation.QuestionBankDtos.TagView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 질문 은행 태그(ROLE_ADMIN). 자동완성과 "없으면 만들기". */
@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

  private final TagService service;

  public AdminTagController(TagService service) {
    this.service = service;
  }

  @GetMapping
  public List<TagView> suggest(@RequestParam(defaultValue = "") String query) {
    return service.suggest(query).stream().map(TagView::of).toList();
  }

  /** 새로 만들면 201, 이미 있던 태그면 200(같은 태그를 돌려준다). */
  @PostMapping
  public ResponseEntity<TagResult> create(@Valid @RequestBody CreateTagRequest body) {
    TagService.Result r = service.findOrCreate(body.name());
    return ResponseEntity.status(r.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(new TagResult(TagView.of(r.tag()), r.created()));
  }
}
