package com.team7.cafe.delivery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery-batches")
public class DeliveryController {

    /** A14 배송 묶음 */
    @GetMapping
    public Object batches(@RequestParam String shippingDate) {
        throw new UnsupportedOperationException("A14 미구현");
    }
}
