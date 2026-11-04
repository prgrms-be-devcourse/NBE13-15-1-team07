package com.team7.cafe.delivery.dto;

import java.time.LocalDate;
import java.util.List;

/** A14 배송 묶음 한 건. 이메일·배송일이 같은 PLACED 주문을 묶은 결과. */
public record DeliveryBatchResponse(
        LocalDate shippingDate,
        String email,
        String address,
        String postcode,
        List<Long> orderIds,
        int orderCount,
        List<DeliveryItemResponse> items,
        long totalAmount,
        long totalQuantity) {
}
