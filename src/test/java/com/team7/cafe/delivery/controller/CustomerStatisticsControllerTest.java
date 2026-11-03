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

import com.team7.cafe.delivery.dto.CustomerStatisticsResponse;
import com.team7.cafe.delivery.service.StatisticsService;

/** 공통 오류 본문 변환과 405는 A17 컨트롤러 테스트에서 확인하므로 여기서는 A24 고유 응답만 본다. */
@WebMvcTest(StatisticsController.class)
@DisplayName("A24 접수일별 고객 통계 컨트롤러")
class CustomerStatisticsControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StatisticsService statisticsService;

    @Test
    @DisplayName("200과 고객 통계 배열을 돌려주고, 결과가 없으면 빈 배열이다")
    void customerStatistics() throws Exception {
        // given: 서비스가 11-03에는 고객 2명을, 01-01에는 빈 목록을 돌려준다
        when(statisticsService.customerStatistics("2026-11-03")).thenReturn(List.of(
                new CustomerStatisticsResponse(DATE, "a@x.com", 3, 2, 1, 5, 27000),
                new CustomerStatisticsResponse(DATE, "b@x.com", 1, 0, 1, 0, 0)));
        when(statisticsService.customerStatistics("2026-01-01")).thenReturn(List.of());

        // when & then: 결과가 있는 날
        mockMvc.perform(get("/api/v1/statistics/customers").param("date", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$[0].email").value("a@x.com"))
                .andExpect(jsonPath("$[0].orderCount").value(3))
                .andExpect(jsonPath("$[0].placedOrderCount").value(2))
                .andExpect(jsonPath("$[0].cancelledOrderCount").value(1))
                .andExpect(jsonPath("$[0].totalQuantity").value(5))
                .andExpect(jsonPath("$[0].totalAmount").value(27000))
                .andExpect(jsonPath("$[1].placedOrderCount").value(0));

        // when & then: 결과가 없는 날
        mockMvc.perform(get("/api/v1/statistics/customers").param("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("date 파라미터가 없으면 400 VALIDATION_ERROR와 필드 오류를 돌려준다")
    void missingDate() throws Exception {
        // given: date 파라미터 없이 요청한다

        // when & then
        mockMvc.perform(get("/api/v1/statistics/customers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.date").value("필수 값입니다."));
        verifyNoInteractions(statisticsService);
    }
}
