package com.team7.cafe.delivery.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.team7.cafe.delivery.dto.DeliveryBatchResponse;
import com.team7.cafe.delivery.dto.DeliveryItemResponse;
import com.team7.cafe.delivery.service.DeliveryService;

/** 공통 오류 본문 변환과 405는 A17 컨트롤러 테스트에서 확인하므로 여기서는 A14 고유 응답만 본다. */
@WebMvcTest(DeliveryController.class)
@DisplayName("A14 배송 묶음 컨트롤러")
class DeliveryControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeliveryService deliveryService;

    @Test
    @DisplayName("200과 배송 묶음 배열을 돌려주고, 배송 대상이 없으면 빈 배열이다")
    void batches() throws Exception {
        // given: 서비스가 11-03에는 묶음 1개를, 01-01에는 빈 목록을 돌려준다
        when(deliveryService.findBatches("2026-11-03")).thenReturn(List.of(new DeliveryBatchResponse(
                DATE, "a@x.com", "서울시 실습로 10", "01234", List.of(1L, 2L), 2,
                List.of(new DeliveryItemResponse(1, "Columbia", 5000, 4, 20000),
                        new DeliveryItemResponse(2, "Brazil", 7000, 1, 7000)),
                27000, 5)));
        when(deliveryService.findBatches("2026-01-01")).thenReturn(List.of());

        // when & then: 배송 대상이 있는 날
        mockMvc.perform(get("/api/v1/delivery-batches").param("shippingDate", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].shippingDate").value("2026-11-03"))
                .andExpect(jsonPath("$[0].email").value("a@x.com"))
                .andExpect(jsonPath("$[0].address").value("서울시 실습로 10"))
                .andExpect(jsonPath("$[0].postcode").value("01234"))
                .andExpect(jsonPath("$[0].orderIds[0]").value(1))
                .andExpect(jsonPath("$[0].orderIds[1]").value(2))
                .andExpect(jsonPath("$[0].orderCount").value(2))
                .andExpect(jsonPath("$[0].totalAmount").value(27000))
                .andExpect(jsonPath("$[0].totalQuantity").value(5))
                .andExpect(jsonPath("$[0].items.length()").value(2))
                .andExpect(jsonPath("$[0].items[0].menuId").value(1))
                .andExpect(jsonPath("$[0].items[0].menuName").value("Columbia"))
                .andExpect(jsonPath("$[0].items[0].unitPrice").value(5000))
                .andExpect(jsonPath("$[0].items[0].quantity").value(4))
                .andExpect(jsonPath("$[0].items[0].lineAmount").value(20000));

        // when & then: 배송 대상이 없는 날
        mockMvc.perform(get("/api/v1/delivery-batches").param("shippingDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("shippingDate 파라미터가 없으면 400 VALIDATION_ERROR와 필드 오류를 돌려준다")
    void missingShippingDate() throws Exception {
        // given: shippingDate 파라미터 없이 요청한다

        // when & then
        mockMvc.perform(get("/api/v1/delivery-batches"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.shippingDate").value("필수 값입니다."));
        verifyNoInteractions(deliveryService);
    }
}
