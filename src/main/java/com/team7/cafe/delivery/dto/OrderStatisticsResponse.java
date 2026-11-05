package com.team7.cafe.delivery.dto;

import java.time.LocalDate;

/** A17 접수일별 주문 통계. 주문이 없는 날은 모든 수치가 0. */
public record OrderStatisticsResponse(
        LocalDate date,
        long orderCount,
        long placedOrderCount,
        long cancelledOrderCount,
        long totalQuantity,
        long totalAmount) {

    public static OrderStatisticsResponse empty(LocalDate date) {
        return new OrderStatisticsResponse(date, 0, 0, 0, 0, 0);
    }
}
