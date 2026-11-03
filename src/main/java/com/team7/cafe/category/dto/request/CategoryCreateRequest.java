package com.team7.cafe.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank
        @Size(max=20)
        String name
) {
}
