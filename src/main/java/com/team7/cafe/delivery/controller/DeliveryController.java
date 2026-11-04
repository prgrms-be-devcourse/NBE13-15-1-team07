package com.team7.cafe.delivery.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team7.cafe.delivery.dto.DeliveryBatchResponse;
import com.team7.cafe.delivery.service.DeliveryService;

@RestController
@RequestMapping("/api/v1/delivery-batches")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /** A14 배송 묶음 */
    @GetMapping
    public List<DeliveryBatchResponse> batches(@RequestParam String shippingDate) {
        return deliveryService.findBatches(shippingDate);
    }
}
