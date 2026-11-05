package com.team7.cafe.delivery.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team7.cafe.delivery.dto.DeliveryBatchResponse;
import com.team7.cafe.delivery.dto.PageResponse;
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

    /** A14 배송 묶음 페이징 (심화): 기존 목록 응답은 그대로 두고 별도 경로로 제공한다. 페이지 크기는 묶음 수다. */
    @GetMapping("/page")
    public PageResponse<DeliveryBatchResponse> batchesPage(@RequestParam String shippingDate,
            @RequestParam(required = false) String page, @RequestParam(required = false) String size) {
        return deliveryService.findBatchesPage(shippingDate, page, size);
    }
}
