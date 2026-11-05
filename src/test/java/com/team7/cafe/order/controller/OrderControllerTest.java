package com.team7.cafe.order.controller;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.order.OrderErrorCode;
import com.team7.cafe.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("없는 주문을 상세 조회하면 404, ORDER_NOT_FOUND, fieldErrors는 빈 객체")
    void getOrder_notFound_returns404() throws Exception {
        given(orderService.getOrder(999L)).willThrow(new ApiException(OrderErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    @DisplayName("취소 마감이 지난 주문을 취소하면 409, ORDER_NOT_CANCELLABLE, fieldErrors는 빈 객체")
    void cancel_afterDeadline_returns409() throws Exception {
        given(orderService.cancel(1L)).willThrow(new ApiException(OrderErrorCode.ORDER_NOT_CANCELLABLE));

        mockMvc.perform(post("/api/v1/orders/1/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"))
                .andExpect(jsonPath("$.message").value("배송 대상일 14시 전의 접수 상태 주문만 취소할 수 있습니다."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
}
