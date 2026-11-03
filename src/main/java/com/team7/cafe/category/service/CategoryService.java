package com.team7.cafe.category.service;

import com.team7.cafe.category.CategoryErrorCode;
import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.repository.CategoryRepository;
import com.team7.cafe.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryResponse createCategory(CategoryCreateRequest request){
        if (categoryRepository.existsByName(request.name())) {
            throw new ApiException(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
        }
        Category category = new Category(request.name());
        Category savedCategory = categoryRepository.save(category);

        return CategoryResponse.from(savedCategory);
    }

    public CategoryResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        return CategoryResponse.from(category);
    }

    public List<CategoryResponse> getCategories() {
        List<Category> list = categoryRepository.findAll();
        return list.stream()
                .map(category -> CategoryResponse.from(category))
                .toList();
    }



}
