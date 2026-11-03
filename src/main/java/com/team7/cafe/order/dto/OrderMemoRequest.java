package com.team7.cafe.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderMemoRequest(
        @NotNull(message = "메모는 필수입니다.")
        @Size(max = 200, message = "메모는 200자 이하여야 합니다.")
        String memo
        ) {
}
