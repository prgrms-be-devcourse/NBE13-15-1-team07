package com.team7.cafe.menu;

import com.team7.cafe.menu.controller.MenuController;
import com.team7.cafe.menu.dto.MenuPageResponse;
import com.team7.cafe.menu.dto.MenuResponse;
import com.team7.cafe.menu.service.MenuService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    // [3] 심화 정상 처리: 메뉴 페이징 조회는 200이고 content, number, size, totalElements, totalPages 5개 필드를 돌려준다 [2026.11.06/안형빈]
    @Test
    @DisplayName("메뉴 페이징 조회는 200과 content·number·size·totalElements·totalPages를 돌려준다")
    void getMenusPage_returns200WithPageFields() throws Exception {
        MenuPageResponse response = new MenuPageResponse(
                List.of(
                        new MenuResponse(1L, 1L, "Columbia Nariñó", 5000, "설명1", true),
                        new MenuResponse(2L, 1L, "Brazil Serra Do Caparaó", 7000, "설명2", false)
                ),
                0, 2, 5L, 3);
        when(menuService.getMenusPage(0, 2)).thenReturn(response);

        mockMvc.perform(get("/api/v1/menus/page").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].available").value(false))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(menuService).getMenusPage(0, 2);
    }

    // [4] 심화 기본값: page, size를 보내지 않으면 page 0, size 10으로 조회한다 [2026.11.06/안형빈]
    @Test
    @DisplayName("메뉴 페이징 조회에 page·size가 없으면 기본값 0과 10으로 조회한다")
    void getMenusPage_noParams_usesDefaults() throws Exception {
        when(menuService.getMenusPage(0, 10)).thenReturn(new MenuPageResponse(List.of(), 0, 10, 0L, 0));

        mockMvc.perform(get("/api/v1/menus/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(menuService).getMenusPage(0, 10);
    }

    // [5] 심화 입력 오류: size가 1~50 범위를 벗어나면 400 VALIDATION_ERROR이고 fieldErrors에 size가 담긴다 [2026.11.06/안형빈]
    @Test
    @DisplayName("메뉴 페이징 조회에 size가 범위를 벗어나면 400, VALIDATION_ERROR, fieldErrors에 size")
    void getMenusPage_invalidSize_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/menus/page").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors.size").exists());

        // 거절된 요청은 서비스까지 전달되지 않는다
        verify(menuService, never()).getMenusPage(anyInt(), anyInt());
    }

    // [6] 심화 입력 오류: page와 size가 모두 틀리면 fieldErrors에 두 키가 한꺼번에 담긴다 [2026.11.06/안형빈]
    @Test
    @DisplayName("메뉴 페이징 조회에 page와 size가 모두 틀리면 fieldErrors에 page와 size가 함께 담긴다")
    void getMenusPage_invalidPageAndSize_returnsBothFieldErrors() throws Exception {
        mockMvc.perform(get("/api/v1/menus/page").param("page", "-1").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.page").exists())
                .andExpect(jsonPath("$.fieldErrors.size").exists());

        verify(menuService, never()).getMenusPage(anyInt(), anyInt());
    }
}
