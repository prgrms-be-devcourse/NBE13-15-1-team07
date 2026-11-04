package com.team7.cafe.delivery.dto;

import java.time.LocalDate;

/** A24 접수일별 고객 통계 한 줄. 모두 취소한 고객도 포함하며 그 고객의 수량·금액은 0. */
public record CustomerStatisticsResponse(
        LocalDate date,
        String email,
        long orderCount,
        long placedOrderCount,
        long cancelledOrderCount,
        long totalQuantity,
        long totalAmount) {
}
