package com.team7.cafe.category.controller;

import com.team7.cafe.category.CategoryErrorCode;
import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.request.CategoryUpdateRequest;
import com.team7.cafe.category.dto.response.CategoryPageResponse;
import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.service.CategoryService;
import com.team7.cafe.global.error.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated //심화 : @RequestParam의 @Min, @Max 검증을 위해 필수 선언
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request){
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(
            @PathVariable Long categoryId){
        CategoryResponse response = categoryService.getCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> list = categoryService.getCategories();
        return ResponseEntity.ok(list);
    }

    //if : category에 page기술을 도입한다면
    @GetMapping("/page")
    public ResponseEntity<CategoryPageResponse> getCategoriesWithPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // 도메인 내부에서 검증 및 ApiException 발생
        if (page < 0) {
            throw new ApiException(CategoryErrorCode.INVALID_PAGE_NUMBER);
        }
        if (size < 1 || size > 50) {
            throw new ApiException(CategoryErrorCode.INVALID_PAGE_SIZE);
        }

        CategoryPageResponse response = categoryService.getCategories(page, size);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request){
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }


}
