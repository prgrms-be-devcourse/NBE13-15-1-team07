package com.team7.cafe.global.error;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> fieldErrors;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    public ApiException(ErrorCode errorCode, Map<String, String> fieldErrors) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    public ErrorCode errorCode() { return errorCode; }
    public Map<String, String> fieldErrors() { return fieldErrors; }
}
