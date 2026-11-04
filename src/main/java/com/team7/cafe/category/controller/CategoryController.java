package com.team7.cafe.category.controller;

import com.team7.cafe.category.dto.request.CategoryCreateRequest;
import com.team7.cafe.category.dto.response.CategoryResponse;
import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryCreateRequest request){
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }


}
