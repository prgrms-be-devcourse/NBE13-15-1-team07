package com.team7.cafe.category.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record CategoryPageResponse (List<CategoryResponse> content,
                                    int number,
                                    int size,
                                    long totalElements,
                                    int totalPages
){
    public static CategoryPageResponse from(Page<CategoryResponse> page) {
        return new CategoryPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
