package com.team7.cafe.menu;

import org.springframework.http.HttpStatus;

public enum MenuErrorCode implements com.team7.cafe.global.error.ErrorCode {
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    MENU_IN_USE(HttpStatus.CONFLICT, "주문에 사용된 메뉴는 삭제할 수 없습니다."),
    MENU_UNAVAILABLE(HttpStatus.CONFLICT, "판매 중지된 메뉴가 포함되어 있습니다.");

    private final HttpStatus status;
    private final String message;

    MenuErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return name(); }
    @Override public String message() { return message; }
}
