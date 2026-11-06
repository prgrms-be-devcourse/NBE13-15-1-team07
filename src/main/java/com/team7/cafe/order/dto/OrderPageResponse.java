package com.team7.cafe.order.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record OrderPageResponse(
        List<OrderSummaryResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages
) {

    public static OrderPageResponse from(Page<OrderSummaryResponse> page) {
        return new OrderPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
