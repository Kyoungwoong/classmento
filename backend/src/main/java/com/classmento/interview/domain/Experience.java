package com.classmento.interview.domain;

import java.util.List;

/** 면접에 쓰는 이력서 경험(이력서 컨텍스트에서 받아 온 값). embedding은 질문 은행 kNN 질의용. */
public record Experience(
    String title, String summary, String detail, List<String> skills, float[] embedding) {}
