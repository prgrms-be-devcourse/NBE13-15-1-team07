package com.team7.cafe.order.dto;

import com.team7.cafe.order.entity.Order;
import com.team7.cafe.order.entity.OrderStatus;

import java.time.LocalDate;

public record OrderSummaryResponse(
        Long id,
        String createdAt,
        LocalDate shippingDate,
        long totalAmount,
        OrderStatus status
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                ResponseTime.format(order.getCreatedAt()),
                order.getShippingDate(),
                order.getTotalAmount(),
                order.getStatus()
        );
    }
}
