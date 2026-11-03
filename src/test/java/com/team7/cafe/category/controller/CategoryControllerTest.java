package com.team7.cafe.category.controller;

import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(get("/categories")
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
        mockMvc.perform(get("/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("커피"));
    }
}