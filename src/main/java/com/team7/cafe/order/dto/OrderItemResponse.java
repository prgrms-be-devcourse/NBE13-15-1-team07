package com.team7.cafe.order.dto;

import com.team7.cafe.order.entity.OrderItem;

public record OrderItemResponse(
        Long menuId,
        String menuName,
        int unitPrice,
        int quantity,
        long lineAmount
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getMenuId(),
                item.getMenuName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineAmount()
        );
    }
}
