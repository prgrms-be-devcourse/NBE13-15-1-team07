package com.team7.cafe.order;

import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements com.team7.cafe.global.error.ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ADDRESS_MISMATCH(HttpStatus.CONFLICT, "같은 이메일과 배송일의 기존 주문과 배송지가 다릅니다."),
    ORDER_NOT_CANCELLABLE(HttpStatus.CONFLICT, "배송 대상일 14시 전의 접수 상태 주문만 취소할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    OrderErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return name(); }
    @Override public String message() { return message; }
}
