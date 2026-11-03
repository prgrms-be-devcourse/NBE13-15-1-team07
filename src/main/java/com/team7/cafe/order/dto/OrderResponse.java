package com.team7.cafe.order.dto;

import com.team7.cafe.order.entity.Order;
import com.team7.cafe.order.entity.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        Long id,
        String email,
        String address,
        String postcode,
        String createdAt,
        LocalDate shippingDate,
        long totalAmount,
        List<OrderItemResponse> items,
        OrderStatus status,
        String cancelledAt,
        String memo
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(OrderItemResponse::from).toList();

        return new OrderResponse(
                order.getId(),
                order.getEmail(),
                order.getAddress(),
                order.getPostcode(),
                ResponseTime.format(order.getCreatedAt()),
                order.getShippingDate(),
                order.getTotalAmount(),
                items,
                order.getStatus(),
                ResponseTime.format(order.getCancelledAt()),
                order.getMemo()
        );
    }
}
