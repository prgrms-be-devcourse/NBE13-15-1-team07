package com.team7.cafe.global.error;

import java.util.Map;

/** 프론트(api.js)가 읽는 형식: { code, message, fieldErrors } */
public record ErrorResponse(String code, String message, Map<String, String> fieldErrors) {

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode.code(), errorCode.message(), fieldErrors);
    }
}
