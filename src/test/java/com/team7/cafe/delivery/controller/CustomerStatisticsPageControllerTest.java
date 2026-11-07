package com.team7.cafe.delivery.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.team7.cafe.delivery.dto.CustomerStatisticsResponse;
import com.team7.cafe.delivery.dto.MenuStatisticsResponse;
import com.team7.cafe.delivery.dto.PageResponse;
import com.team7.cafe.delivery.service.StatisticsService;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

@WebMvcTest(StatisticsController.class)
@DisplayName("A24 접수일별 고객 통계 페이징 컨트롤러")
class CustomerStatisticsPageControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StatisticsService statisticsService;

    @Test
    @DisplayName("200과 {content, number, size, totalElements, totalPages}를 돌려준다")
    void page() throws Exception {
        // given
        when(statisticsService.customerStatisticsPage("2026-11-03", "1", "2")).thenReturn(new PageResponse<>(
                List.of(new CustomerStatisticsResponse(DATE, "c@x.com", 3, 2, 1, 5, 27000)), 1, 2, 3, 2));

        // when & then
        mockMvc.perform(get("/api/v1/statistics/customers/page")
                        .param("date", "2026-11-03").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("c@x.com"))
                .andExpect(jsonPath("$.content[0].cancelledOrderCount").value(1))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("A18 /menus/page도 200과 {content, number, size, totalElements, totalPages}를 돌려준다")
    void menusPage() throws Exception {
        // given
        when(statisticsService.menuStatisticsPage("2026-11-03", "0", "2")).thenReturn(new PageResponse<>(
                List.of(new MenuStatisticsResponse(1, "Columbia", 5000, 3, 15000)), 0, 2, 1, 1));

        // when & then
        mockMvc.perform(get("/api/v1/statistics/menus/page")
                        .param("date", "2026-11-03").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].menuName").value("Columbia"))
                .andExpect(jsonPath("$.content[0].totalQuantity").value(3))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("date가 없으면 400 VALIDATION_ERROR이고 date 필드 오류다")
    void missingDate() throws Exception {
        // given: date 없이 요청한다

        // when & then
        mockMvc.perform(get("/api/v1/statistics/customers/page"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.date").value("필수 값입니다."));
    }

    @Test
    @DisplayName("서비스가 page·size 오류를 던지면 400과 필드 오류 본문으로 바꿔 준다")
    void invalidPageBecomesErrorBody() throws Exception {
        // given
        when(statisticsService.customerStatisticsPage("2026-11-03", "-1", "0")).thenThrow(new ApiException(
                CommonErrorCode.VALIDATION_ERROR,
                Map.of("page", "0 이상의 정수여야 합니다.", "size", "1~50 사이의 정수여야 합니다.")));

        // when & then
        mockMvc.perform(get("/api/v1/statistics/customers/page")
                        .param("date", "2026-11-03").param("page", "-1").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors.page").value("0 이상의 정수여야 합니다."))
                .andExpect(jsonPath("$.fieldErrors.size").value("1~50 사이의 정수여야 합니다."));
    }

}
