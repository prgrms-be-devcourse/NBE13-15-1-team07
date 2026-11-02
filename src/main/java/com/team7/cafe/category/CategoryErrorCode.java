package com.team7.cafe.category;

import org.springframework.http.HttpStatus;

public enum CategoryErrorCode implements com.team7.cafe.global.error.ErrorCode {
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_NAME_DUPLICATED(HttpStatus.CONFLICT, "같은 이름의 카테고리가 이미 있습니다."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "메뉴가 등록된 카테고리는 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    CategoryErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return name(); }
    @Override public String message() { return message; }
}
