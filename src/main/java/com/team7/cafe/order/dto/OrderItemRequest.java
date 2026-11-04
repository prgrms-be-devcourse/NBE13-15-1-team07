package com.team7.cafe.order.dto;

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
}
