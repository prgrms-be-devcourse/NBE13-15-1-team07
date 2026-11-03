package com.team7.cafe.category.repository;

import com.team7.cafe.category.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("카테고리 저장 및 조회 검증")
    void saveAndFindCategory() {
        // given
        Category category = new Category("커피");

        // when
        Category savedCategory = categoryRepository.save(category);

        // then
        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("커피");
    }

    @Test
    @DisplayName("이름 중복 검증 - existsByName")
    void existsByNameTest() {
        // given
        categoryRepository.save(new Category("디저트"));

        // when & then
        assertThat(categoryRepository.existsByName("디저트")).isTrue();
        assertThat(categoryRepository.existsByName("논커피")).isFalse();
    }

    @Test
    @DisplayName("카테고리 이름 중복 저장 시 예외 발생 검증")
    void duplicateCategoryNameThrowsException() {
        // given
        categoryRepository.save(new Category("베이커리"));

        // when & then
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new Category("베이커리")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}