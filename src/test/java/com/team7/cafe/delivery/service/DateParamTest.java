package com.team7.cafe.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

@DisplayName("URL 날짜 검사")
class DateParamTest {

    @Test
    @DisplayName("범위의 양 끝과 윤일은 통과한다")
    void validBoundaries() {
        assertThat(DateParam.parse("date", "2000-01-01")).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(DateParam.parse("date", "2099-12-31")).isEqualTo(LocalDate.of(2099, 12, 31));
        assertThat(DateParam.parse("date", "2024-02-29")).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {
            "1999-12-31",     // 범위 아래
            "2100-01-01",     // 범위 위
            "2026-02-30",     // 없는 날짜
            "2025-02-29",     // 평년 윤일
            "2026-13-01",     // 없는 달
            "2026-1-3",       // 자리수 부족
            "20261103",       // 하이픈 없음
            "2026/11/03",     // 구분자 다름
            " 2026-11-03",    // 앞 공백
            "2026-11-03 ",    // 뒤 공백
            "2026-11-03\n",   // 끝 줄바꿈
            "２０２６-１１-０３",   // 전각 숫자
            "abc"
    })
    @DisplayName("올바르지 않은 날짜는 VALIDATION_ERROR와 필드 오류를 돌려준다")
    void invalid(String value) {
        assertThatThrownBy(() -> DateParam.parse("date", value))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
                    assertThat(e.fieldErrors()).containsKey("date");
                });
    }

    @Test
    @DisplayName("필드 오류의 키는 호출자가 넘긴 파라미터 이름이다")
    void fieldNameIsCallerSupplied() {
        assertThatThrownBy(() -> DateParam.parse("shippingDate", "abc"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.fieldErrors()).containsOnlyKeys("shippingDate"));
    }
}
