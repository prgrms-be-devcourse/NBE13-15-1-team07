package com.team7.cafe.category.service;

import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.repository.CategoryRepository;
import com.team7.cafe.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;
    @Nested
    @DisplayName("카테고리 전체 목록 조회 (A01)")
    class GetCategoriesTest {

        @Test
        @DisplayName("성공: 등록된 카테고리 목록이 존재하면 ID 오름차순 DTO 리스트로 반환된다.")
        void getCategories_success() {
            // given
            Category category1 = new Category("커피");
            ReflectionTestUtils.setField(category1, "id", 1L);

            Category category2 = new Category("디저트");
            ReflectionTestUtils.setField(category2, "id", 2L);

            given(categoryRepository.findAllByOrderByIdAsc()).willReturn(List.of(category1, category2));

            // when
            List<CategoryResponse> responses = categoryService.getCategories();

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo(1L);
            assertThat(responses.get(0).name()).isEqualTo("커피");
            assertThat(responses.get(1).id()).isEqualTo(2L);
            assertThat(responses.get(1).name()).isEqualTo("디저트");

            verify(categoryRepository).findAllByOrderByIdAsc();
        }

        @Test
        @DisplayName("성공: 등록된 카테고리가 없으면 빈 리스트를 반환한다.")
        void getCategories_empty() {
            // given
            given(categoryRepository.findAllByOrderByIdAsc()).willReturn(List.of());

            // when
            List<CategoryResponse> responses = categoryService.getCategories();

            // then
            assertThat(responses).isNotNull();
            assertThat(responses).isEmpty();

            verify(categoryRepository).findAllByOrderByIdAsc();
        }
    }

    @Nested
    @DisplayName("카테고리 등록 (A03)")
    class CreateCategoryTest {

        @Test
        @DisplayName("성공: 중복되지 않은 이름으로 요청 시 정상 등록된다.")
        void createCategory_success() {
            // given
            String categoryName = "커피";
            CategoryCreateRequest request = new CategoryCreateRequest(categoryName);
            Category category = new Category(categoryName);
            ReflectionTestUtils.setField(category, "id", 1L);

            given(categoryRepository.existsByName(categoryName)).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willReturn(category);

            // when
            CategoryResponse response = categoryService.createCategory(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo(categoryName);

            verify(categoryRepository).existsByName(categoryName);
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 카테고리 이름이면 ApiException 예외가 발생하고 저장을 호출하지 않는다.")
        void createCategory_duplicate_fail() {
            // given
            String duplicateName = "커피";
            CategoryCreateRequest request = new CategoryCreateRequest(duplicateName);

            given(categoryRepository.existsByName(duplicateName)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(ApiException.class);

            verify(categoryRepository).existsByName(duplicateName);
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }
}