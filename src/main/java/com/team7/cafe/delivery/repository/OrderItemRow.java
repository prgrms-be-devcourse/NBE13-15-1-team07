package com.team7.cafe.delivery.repository;

/** 주문 한 줄 + 그 주문의 항목 한 줄. JPQL의 select new가 직접 만들기 때문에 별도 최상위 타입으로 둔다. */
public record OrderItemRow(
        long orderId,
        String email,
        String address,
        String postcode,
        String status,
        long menuId,
        String menuName,
        int unitPrice,
        int quantity,
        long lineAmount) {
}
