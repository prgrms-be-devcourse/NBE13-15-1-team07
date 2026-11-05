package com.team7.cafe.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "이메일은 필수입니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        @Pattern(regexp = OrderCreateRequest.EMAIL_REGEX, message = "이메일 형식을 확인해 주세요.")
        String email,

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
        String address,

        @NotNull(message = "우편번호는 필수입니다.")
        @Pattern(regexp = "[0-9]{5}", message = "우편번호는 숫자 5자리여야 합니다.")
        String postcode,

        @NotNull(message = "주문 항목은 필수입니다.")
        @Size(min = 1, max = 10, message = "주문 항목은 1~10개여야 합니다.")
        List<@Valid OrderItemRequest> items
        ) {

        public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$";

        public OrderCreateRequest{
                address = (address == null) ? null : address.trim();
                postcode = (postcode == null) ? null : postcode.trim();
        }
}
