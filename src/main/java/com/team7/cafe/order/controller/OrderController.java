package com.team7.cafe.order.controller;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;
import com.team7.cafe.order.dto.OrderCreateRequest;
import com.team7.cafe.order.dto.OrderMemoRequest;
import com.team7.cafe.order.dto.OrderResponse;
import com.team7.cafe.order.dto.OrderSummaryResponse;
import com.team7.cafe.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getOrders(@RequestParam("email") String email) {
        validateEmail(email);
        return ResponseEntity.ok(orderService.getOrders(email));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("orderId") Long orderId) {
        validateOrderId(orderId);
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable("orderId") Long orderId) {
        validateOrderId(orderId);
        return ResponseEntity.ok(orderService.cancel(orderId));
    }

    @PutMapping("/{orderId}/memo")
    public ResponseEntity<OrderResponse> updateMemo(@PathVariable("orderId") Long orderId,
                                                    @Valid @RequestBody OrderMemoRequest request) {
        validateOrderId(orderId);
        return ResponseEntity.ok(orderService.updateMemo(orderId, request));
    }

    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<OrderResponse> reorder(@PathVariable("orderId") Long orderId) {
        validateOrderId(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.reorder(orderId));
    }

    private void validateOrderId(Long orderId) {
        if (orderId < 1) {
            throw new ApiException(CommonErrorCode.VALIDATION_ERROR,
                    Map.of("orderId", "주문 ID는 1 이상의 정수여야 합니다."));
        }
    }

    private void validateEmail(String email) {
        if (email.length() > 100 || !email.matches(OrderCreateRequest.EMAIL_REGEX)) {
            throw new ApiException(CommonErrorCode.VALIDATION_ERROR,
                    Map.of("email", "이메일 형식을 확인해 주세요."));
        }
    }
}
