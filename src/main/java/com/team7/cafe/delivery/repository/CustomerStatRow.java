package com.team7.cafe.delivery.repository;

/**
 * A24 페이징용: 고객 한 명의 하루 집계를 DB(GROUP BY)가 계산해 돌려준 한 줄.
 * 수량·금액은 PLACED 주문만 더한 값이고, 취소 수는 orderCount - placedOrderCount로 구한다.
 */
public record CustomerStatRow(
        String email,
        long orderCount,
        long placedOrderCount,
        long totalQuantity,
        long totalAmount) {
}
