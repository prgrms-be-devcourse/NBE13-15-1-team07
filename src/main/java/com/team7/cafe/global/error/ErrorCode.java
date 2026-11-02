package com.team7.cafe.global.error;

import org.springframework.http.HttpStatus;

/** 도메인별 오류 코드 enum이 구현한다. 공통 코드는 CommonErrorCode. */
public interface ErrorCode {

    HttpStatus status();

    String code();

    String message();
}
