package com.classmento.interview.domain;

/** 기준 하나의 점수(1~5)와 답변에서 인용한 근거. */
public record Score(int score, String evidence) {}
