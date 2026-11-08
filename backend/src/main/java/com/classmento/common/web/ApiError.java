package com.classmento.common.web;

/** 모든 API 에러 응답의 공통 형태. message는 사용자에게 그대로 보여줄 수 있는 한국어 문장이어야 한다. */
public record ApiError(String code, String message) {}
