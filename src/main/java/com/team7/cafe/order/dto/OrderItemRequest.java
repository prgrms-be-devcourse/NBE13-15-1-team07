package com.team7.cafe.order.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "메뉴 ID는 필수입니다.")
        @Min(value = 1, message = "메뉴 ID는 1 이상이어야 합니다.")
        Long menuId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1~20 사이여야 합니다.")
        @Max(value = 20, message = "수량은 1~20 사이여야 합니다.")
        Integer quantity
) {

    // 명세 5절: menuId, quantity는 JSON 숫자여야 한다
    // Jackson은 기본으로 "2" 같은 문자열을 숫자로 바꿔 주기 때문에,
    // 바꾸기 전 원래 값(Object)을 받아 정수인지 직접 확인한 뒤 위 기본 생성자로 만든다
    @JsonCreator
    public static OrderItemRequest fromJson(@JsonProperty("menuId") Object menuId,
                                            @JsonProperty("quantity") Object quantity) {
        return new OrderItemRequest(toLong(menuId), toInteger(quantity));
    }

    // JSON 정수는 Integer나 Long으로 들어온다. 문자열, 소수는 거절
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("menuId는 JSON 숫자여야 합니다.");
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new IllegalArgumentException("quantity는 JSON 정수여야 합니다.");
    }
}
