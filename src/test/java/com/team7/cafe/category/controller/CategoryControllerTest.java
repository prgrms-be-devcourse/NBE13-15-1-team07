package com.team7.cafe.category.controller;

import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Jackson ObjectMapper import
import tools.jackson.databind.ObjectMapper;

// Mockito ArgumentMatchers static import (eq, any)
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

// MockMvc static import (put, status, jsonPath 등)
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//willDoNothing()
import static org.mockito.BDDMockito.willDoNothing;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("A01: 카테고리 전체 목록 조회 성공 - 200 OK")
    void getCategories_Success() throws Exception {
        // given
        CategoryResponse response1 = new CategoryResponse(1L, "커피");
        CategoryResponse response2 = new CategoryResponse(2L, "디저트");
        given(categoryService.getCategories()).willReturn(List.of(response1, response2));

        // when & then
        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("커피"))
                .andExpect(jsonPath("$[1].name").value("디저트"));
    }

    @Test
    @DisplayName("A02: 카테고리 단건 상세 조회 성공 - 200 OK")
    void getCategory_Success() throws Exception {
        // given
        Long categoryId = 1L;
        CategoryResponse response = new CategoryResponse(categoryId, "커피");
        given(categoryService.getCategory(categoryId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("커피"));
    }

    @Nested
    @DisplayName("카테고리 수정 API (PUT /categories/{categoryId})")
    class UpdateCategoryApiTest {

        @Test
        @DisplayName("성공: 올바른 요청 본문과 ID로 요청 시 200 OK와 수정된 카테고리 정보를 반환한다.")
        void updateCategory_success() throws Exception {
            // given
            Long categoryId = 1L;
            CategoryCreateRequest request = new CategoryCreateRequest("디저트");
            CategoryResponse response = new CategoryResponse(categoryId, "디저트");

            given(categoryService.updateCategory(eq(categoryId), any(CategoryCreateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/categories/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(categoryId))
                    .andExpect(jsonPath("$.name").value("디저트"));
        }

        @Test
        @DisplayName("실패: 카테고리 이름이 공백이면 400 Bad Request를 반환한다.")
        void updateCategory_blankName_badRequest() throws Exception {
            // given
            Long categoryId = 1L;
            CategoryCreateRequest request = new CategoryCreateRequest("   ");

            // when & then
            mockMvc.perform(put("/api/v1/categories/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("성공: 존재하는 카테고리 삭제 요청 시 204 No Content를 반환한다.")
        void deleteCategory_success() throws Exception {
            // given
            Long categoryId = 1L;
            willDoNothing().given(categoryService).deleteCategory(categoryId);

            // when & then
            mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId))
                    .andExpect(status().isNoContent());

            verify(categoryService).deleteCategory(categoryId);
        }

    }
}