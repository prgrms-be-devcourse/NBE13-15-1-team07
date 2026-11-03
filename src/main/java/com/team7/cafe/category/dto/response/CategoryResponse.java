package com.team7.cafe.category.dto.response;

import com.team7.cafe.category.entity.Category;

public record CategoryResponse(
        Long id,
        String name
) {

    public static CategoryResponse from(Category category){

        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }

}
