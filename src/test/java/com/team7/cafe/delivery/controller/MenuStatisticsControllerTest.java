package com.team7.cafe.delivery.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.team7.cafe.delivery.dto.MenuStatisticsResponse;
import com.team7.cafe.delivery.service.StatisticsService;

/** 공통 오류 본문 변환과 405는 A17 컨트롤러 테스트에서 확인하므로 여기서는 A18 고유 응답만 본다. */
@WebMvcTest(StatisticsController.class)
@DisplayName("A18 접수일별 메뉴 통계 컨트롤러")
class MenuStatisticsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StatisticsService statisticsService;

    @Test
    @DisplayName("200과 메뉴 통계 배열을 돌려주고, 결과가 없으면 빈 배열이다")
    void menuStatistics() throws Exception {
        // given: 서비스가 11-03에는 메뉴 2개를, 01-01에는 빈 목록을 돌려준다
        when(statisticsService.menuStatistics("2026-11-03")).thenReturn(List.of(
                new MenuStatisticsResponse(1, "Columbia", 5000, 4, 20000),
                new MenuStatisticsResponse(2, "Brazil", 7000, 1, 7000)));
        when(statisticsService.menuStatistics("2026-01-01")).thenReturn(List.of());

        // when & then: 결과가 있는 날
        mockMvc.perform(get("/api/v1/statistics/menus").param("date", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].menuId").value(1))
                .andExpect(jsonPath("$[0].menuName").value("Columbia"))
                .andExpect(jsonPath("$[0].unitPrice").value(5000))
                .andExpect(jsonPath("$[0].totalQuantity").value(4))
                .andExpect(jsonPath("$[0].totalAmount").value(20000))
                .andExpect(jsonPath("$[1].menuId").value(2));

        // when & then: 결과가 없는 날
        mockMvc.perform(get("/api/v1/statistics/menus").param("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("date 파라미터가 없으면 400 VALIDATION_ERROR와 필드 오류를 돌려준다")
    void missingDate() throws Exception {
        // given: date 파라미터 없이 요청한다

        // when & then
        mockMvc.perform(get("/api/v1/statistics/menus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.date").value("필수 값입니다."));
        verifyNoInteractions(statisticsService);
    }
}
