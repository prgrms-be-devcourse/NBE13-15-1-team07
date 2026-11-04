package com.team7.cafe.delivery.dto;

/** 배송 묶음 안의 합산 항목. 메뉴 ID·당시 메뉴명·단가가 모두 같은 항목만 합산한다. */
public record DeliveryItemResponse(
        long menuId,
        String menuName,
        int unitPrice,
        int quantity,
        long lineAmount) {
}
