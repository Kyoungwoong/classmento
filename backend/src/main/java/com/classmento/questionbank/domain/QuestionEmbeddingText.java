package com.classmento.questionbank.domain;

import java.util.List;

/** 임베딩에 넣을 문장: "대분류 > 소분류 · 질문 · 태그1, 태그2"(태그는 이름순, 없으면 생략). */
public final class QuestionEmbeddingText {

  private QuestionEmbeddingText() {}

  public static String of(String topName, String subName, String text, List<String> tagNames) {
    StringBuilder sb = new StringBuilder().append(topName).append(" > ").append(subName);
    sb.append(" · ").append(text);
    List<String> tags = tagNames.stream().sorted().toList();
    if (!tags.isEmpty()) {
      sb.append(" · ").append(String.join(", ", tags));
    }
    return sb.toString();
  }
}
