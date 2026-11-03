package com.team7.cafe.delivery.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

/** URL 날짜(yyyy-MM-dd) 검사: 실제 존재하는 날짜이고 2000-01-01 ~ 2099-12-31 범위. */
final class DateParam {

    private static final Pattern FORMAT = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final LocalDate MIN = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX = LocalDate.of(2099, 12, 31);

    private DateParam() {
    }

    static LocalDate parse(String field, String value) {
        if (value != null && FORMAT.matcher(value).matches()) {
            try {
                LocalDate date = LocalDate.parse(value);
                if (!date.isBefore(MIN) && !date.isAfter(MAX)) {
                    return date;
                }
            } catch (DateTimeParseException ignored) {
                // 아래에서 검증 오류로 처리
            }
        }
        throw new ApiException(CommonErrorCode.VALIDATION_ERROR,
                Map.of(field, "올바른 날짜(2000-01-01 ~ 2099-12-31)를 입력해 주세요."));
    }
}
