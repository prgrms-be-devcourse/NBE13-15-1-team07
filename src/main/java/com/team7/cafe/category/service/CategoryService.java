package com.team7.cafe.category.service;

import com.team7.cafe.category.CategoryErrorCode;
import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.request.CategoryUpdateRequest;
import com.team7.cafe.category.dto.response.CategoryPageResponse;
import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.repository.CategoryRepository;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MenuRepository menuRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request){
        if (categoryRepository.existsByName(request.name())) {
            throw new ApiException(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
        }
        Category category = new Category(request.name());
        Category savedCategory = categoryRepository.save(category);

        return CategoryResponse.from(savedCategory);
    }

    public CategoryResponse getCategory(Long categoryId) {
        Category category = getCategoryOrThrow(categoryId);
        return CategoryResponse.from(category);
    }

    public List<CategoryResponse> getCategories() {
        List<Category> list = categoryRepository.findAllByOrderByIdAsc();
        return list.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    //if : category에 page기술을 도입한다면
    public CategoryPageResponse getCategories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryRepository.findAll(pageable);

        return CategoryPageResponse.from(categoryPage.map(CategoryResponse::from));
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request){
        Category category = getCategoryOrThrow(categoryId);

        String newName = request.name().strip();
        if (!category.getName().equals(newName)) {
            if (categoryRepository.existsByName(newName)) {
                throw new ApiException(CategoryErrorCode.CATEGORY_NAME_DUPLICATED);
            }
            category.updateName(newName);
        }

        return CategoryResponse.from(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId){
        Category category = getCategoryOrThrow(categoryId);
        if (menuRepository.existsByCategoryId(categoryId)) {
            throw new ApiException(CategoryErrorCode.CATEGORY_IN_USE);
        }

        categoryRepository.delete(category);
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

}
