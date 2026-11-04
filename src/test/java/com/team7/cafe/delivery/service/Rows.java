package com.team7.cafe.delivery.service;

import com.team7.cafe.delivery.repository.OrderItemRow;

/** 서비스 테스트에서 리포지토리가 돌려주는 행을 짧게 만들기 위한 도우미. */
final class Rows {

    private Rows() {
    }

    static OrderItemRow row(long orderId, String email, String status, long menuId, String menuName, int unitPrice, int quantity) {
        return new OrderItemRow(orderId, email, "서울시 실습로 10", "01234", status,
                menuId, menuName, unitPrice, quantity, (long) unitPrice * quantity);
    }

    static OrderItemRow placed(long orderId, String email, long menuId, String menuName, int unitPrice, int quantity) {
        return row(orderId, email, "PLACED", menuId, menuName, unitPrice, quantity);
    }

    static OrderItemRow cancelled(long orderId, String email, long menuId, String menuName, int unitPrice, int quantity) {
        return row(orderId, email, "CANCELLED", menuId, menuName, unitPrice, quantity);
    }

    static OrderItemRow withAddress(OrderItemRow base, String address, String postcode) {
        return new OrderItemRow(base.orderId(), base.email(), address, postcode, base.status(),
                base.menuId(), base.menuName(), base.unitPrice(), base.quantity(), base.lineAmount());
    }
}
