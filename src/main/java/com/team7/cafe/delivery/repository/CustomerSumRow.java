package com.team7.cafe.delivery.repository;

/** A24 페이징 2단계: 고객 한 명의 PLACED 주문 수량·금액 합. */
public record CustomerSumRow(String email, long totalQuantity, long totalAmount) {
}
