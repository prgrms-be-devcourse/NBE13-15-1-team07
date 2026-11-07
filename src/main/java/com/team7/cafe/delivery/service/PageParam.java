package com.team7.cafe.delivery.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

/** 페이징 URL 값 검사: page는 0 이상 정수(기본 0), size는 1~50 정수(기본 10). 틀리면 모두 모아 한 번에 400으로 알린다. */
record PageParam(int page, int size) {

    static final int DEFAULT_SIZE = 10;
    static final int MAX_SIZE = 50;
    private static final Pattern DIGITS = Pattern.compile("^\\d{1,9}$");

    static PageParam parse(String page, String size) {
        Map<String, String> errors = new LinkedHashMap<>();

        int pageValue = 0;
        if (page != null) {
            if (DIGITS.matcher(page).matches()) {
                pageValue = Integer.parseInt(page);
            } else {
                errors.put("page", "0 이상의 정수여야 합니다.");
            }
        }

        int sizeValue = DEFAULT_SIZE;
        if (size != null) {
            if (DIGITS.matcher(size).matches() && inRange(Integer.parseInt(size))) {
                sizeValue = Integer.parseInt(size);
            } else {
                errors.put("size", "1~" + MAX_SIZE + " 사이의 정수여야 합니다.");
            }
        }

        if (!errors.isEmpty()) {
            throw new ApiException(CommonErrorCode.VALIDATION_ERROR, errors);
        }
        return new PageParam(pageValue, sizeValue);
    }

    private static boolean inRange(int size) {
        return size >= 1 && size <= MAX_SIZE;
    }
}
