package com.team7.cafe.delivery.repository;

/** A24 페이징 1단계: 고객 한 명의 하루 주문 건수(전체·PLACED). 취소 건수는 둘의 차이다. */
public record CustomerCountRow(String email, long orderCount, long placedOrderCount) {
}
