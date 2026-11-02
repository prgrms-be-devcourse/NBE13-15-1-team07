package com.team7.cafe.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MenuRequest {

    @NotBlank(message = "메뉴 이름은 필수입니다.")
    private String name;

    private String description;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;


    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    public MenuRequest(String name, String description, Integer price, Long categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
    }

    // Getter들 [2026.11.02/안형빈]
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getPrice() {
        return price;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}
