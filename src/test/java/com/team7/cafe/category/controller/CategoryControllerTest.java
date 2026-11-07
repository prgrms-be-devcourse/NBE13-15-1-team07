package com.team7.cafe.category.controller;

import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.request.CategoryUpdateRequest;
import com.team7.cafe.category.dto.response.CategoryPageResponse;
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
            CategoryUpdateRequest request = new CategoryUpdateRequest("디저트");
            CategoryResponse response = new CategoryResponse(categoryId, "디저트");

            given(categoryService.updateCategory(eq(categoryId), any(CategoryUpdateRequest.class)))
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
            CategoryUpdateRequest request = new CategoryUpdateRequest("   ");

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


        ///
        ///심화 : page
        ///

        @Test
        @DisplayName("P01. 카테고리 페이징 목록 조회 - 성공 (기본값 page=0, size=20)")
        void getCategoriesWithPage_Success_Default() throws Exception {
            // given
            List<CategoryResponse> content = List.of(
                    new CategoryResponse(1L, "커피"),
                    new CategoryResponse(2L, "논커피")
            );
            CategoryPageResponse pageResponse = new CategoryPageResponse(content, 0, 20, 2L, 1);
            given(categoryService.getCategories(0, 20)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/categories/page")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("커피"))
                    .andExpect(jsonPath("$.content[1].name").value("논커피"));
        }

        @Test
        @DisplayName("P02. 카테고리 페이징 목록 조회 - 성공 (파라미터 지정 page=1, size=5)")
        void getCategoriesWithPage_Success_CustomParams() throws Exception {
            // given
            CategoryPageResponse pageResponse = new CategoryPageResponse(List.of(), 1, 5, 0L, 0);
            given(categoryService.getCategories(1, 5)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/categories/page")
                            .param("page", "1")
                            .param("size", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("P03. 카테고리 페이징 조회 실패 - size가 최소값(1) 미만인 경우 400 Bad Request")
        void getCategoriesWithPage_Fail_SizeUnderMin() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/categories/page")
                            .param("size", "0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("P04. 카테고리 페이징 조회 실패 - size가 최대값(50) 초과인 경우 400 Bad Request")
        void getCategoriesWithPage_Fail_SizeOverMax() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/categories/page")
                            .param("size", "51")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("P05. 카테고리 페이징 조회 실패 - page가 음수인 경우 400 Bad Request")
        void getCategoriesWithPage_Fail_PageNegative() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/categories/page")
                            .param("page", "-1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

    }
}