package com.team7.cafe.category;

import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.repository.CategoryRepository;
import com.team7.cafe.category.service.CategoryService;
import com.team7.cafe.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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