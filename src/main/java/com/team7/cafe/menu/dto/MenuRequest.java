package com.team7.cafe.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MenuRequest {

    @NotBlank(message = "메뉴 이름은 필수입니다.")
    @Size(min = 1, max = 100, message = "메뉴 이름은 1자 이상 100자 이하여야 합니다.")
    private String name;

    @Size(max = 300, message = "설명은 300자 이하여야 합니다.")
    private String description;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
    @Max(value = 1000000, message = "가격은 1,000,000원 이하여야 합니다.")
    private Integer price;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    public MenuRequest() {
    }

    public MenuRequest(String name, String description, Integer price, Long categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
    }

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