package com.team7.cafe.reference;

import org.springframework.http.HttpStatus;

// [1] 심화: 외부 게시글 조회(RestClient) 오류 코드 3종, 원본 §9 표와 상태·code·message를 그대로 맞춘다 [2026.11.06/안형빈]
public enum ReferenceErrorCode implements com.team7.cafe.global.error.ErrorCode {
    REFERENCE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "외부 게시글을 찾을 수 없습니다."),
    OUTSIDE_API_FAILED(HttpStatus.BAD_GATEWAY, "외부 서버를 호출하지 못했습니다."),
    OUTSIDE_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 서버의 응답 시간이 초과되었습니다.");

    private final HttpStatus status;
    private final String message;

    ReferenceErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return name(); }
    @Override public String message() { return message; }
}
