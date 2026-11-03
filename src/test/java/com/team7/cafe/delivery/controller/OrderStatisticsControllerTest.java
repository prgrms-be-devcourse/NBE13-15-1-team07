package com.team7.cafe.delivery.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.team7.cafe.delivery.dto.OrderStatisticsResponse;
import com.team7.cafe.delivery.service.StatisticsService;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

@WebMvcTest(StatisticsController.class)
@DisplayName("A17 접수일별 주문 통계 컨트롤러")
class OrderStatisticsControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StatisticsService statisticsService;

    @Test
    @DisplayName("200과 주문 통계 필드를 돌려주고(주문 없는 날은 모두 0), 날짜는 받은 문자열 그대로 서비스에 넘긴다")
    void orderStatistics() throws Exception {
        when(statisticsService.orderStatistics("2026-11-03"))
                .thenReturn(new OrderStatisticsResponse(DATE, 3, 2, 1, 5, 27000));

        mockMvc.perform(get("/api/v1/statistics/orders").param("date", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-03"))
                .andExpect(jsonPath("$.orderCount").value(3))
                .andExpect(jsonPath("$.placedOrderCount").value(2))
                .andExpect(jsonPath("$.cancelledOrderCount").value(1))
                .andExpect(jsonPath("$.totalQuantity").value(5))
                .andExpect(jsonPath("$.totalAmount").value(27000));

        verify(statisticsService).orderStatistics("2026-11-03");

        // 주문이 없는 날도 200과 모두 0인 객체다
        when(statisticsService.orderStatistics("2026-01-01")).thenReturn(OrderStatisticsResponse.empty(LocalDate.of(2026, 1, 1)));

        mockMvc.perform(get("/api/v1/statistics/orders").param("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-01-01"))
                .andExpect(jsonPath("$.orderCount").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("date 파라미터가 없으면 400 VALIDATION_ERROR와 필드 오류를 돌려준다")
    void missingDate() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors.date").value("필수 값입니다."));

        verifyNoInteractions(statisticsService);
    }

    @Test
    @DisplayName("서비스가 날짜 오류를 던지면 400과 공통 오류 응답 본문으로 바꿔 준다")
    void invalidDateBecomesErrorBody() throws Exception {
        when(statisticsService.orderStatistics("2026-02-30")).thenThrow(new ApiException(CommonErrorCode.VALIDATION_ERROR,
                Map.of("date", "올바른 날짜(2000-01-01 ~ 2099-12-31)를 입력해 주세요.")));

        mockMvc.perform(get("/api/v1/statistics/orders").param("date", "2026-02-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors.date").value("올바른 날짜(2000-01-01 ~ 2099-12-31)를 입력해 주세요."));
    }

    @Test
    @DisplayName("GET이 아닌 요청은 405를 돌려준다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/statistics/orders").param("date", "2026-11-03"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

        verifyNoInteractions(statisticsService);
    }
}
