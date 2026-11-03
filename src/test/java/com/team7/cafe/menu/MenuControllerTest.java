package com.team7.cafe.menu;

import com.team7.cafe.menu.controller.MenuController;
import com.team7.cafe.menu.service.MenuService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 메뉴 Controller 테스트 : 모의 Service를 사용하고 실제 DB는 연결하지 않는다 [2026.11.03/안형빈]
@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuService menuService;

    // [1] 입력 오류: 메뉴 등록 시 가격이 1~1,000,000 범위를 벗어나면 400 VALIDATION_ERROR이고 fieldErrors에 price가 담긴다 [2026.11.03/안형빈]
    @Test
    @DisplayName("메뉴 등록 시 가격이 범위를 벗어나면 400, VALIDATION_ERROR, fieldErrors에 price")
    void createMenu_invalidPrice_returns400() throws Exception {
        String body = """
                {
                  "categoryId": 1,
                  "name": "가격 오류 메뉴",
                  "price": 0,
                  "description": "가격 범위 위반"
                }
                """;

        mockMvc.perform(post("/api/v1/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors.price").exists());

        // 거절된 요청은 서비스까지 전달되지 않아 데이터가 바뀌지 않는다
        verify(menuService, never()).createMenu(any());
    }

    // [2] 정상 처리: 메뉴 삭제에 성공하면 204이고 응답 본문이 비어 있다 [2026.11.03/안형빈]
    @Test
    @DisplayName("메뉴 삭제에 성공하면 204이고 본문이 비어 있다")
    void deleteMenu_returns204EmptyBody() throws Exception {
        mockMvc.perform(delete("/api/v1/menus/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(menuService).deleteMenu(1L);
    }
}
