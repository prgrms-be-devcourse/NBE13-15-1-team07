package com.team7.cafe.delivery.dto;

/** A18 접수일별 메뉴 통계 한 줄. PLACED 주문만 집계하며 수량이 0인 메뉴는 제외한다. */
public record MenuStatisticsResponse(
        long menuId,
        String menuName,
        int unitPrice,
        long totalQuantity,
        long totalAmount) {
}
