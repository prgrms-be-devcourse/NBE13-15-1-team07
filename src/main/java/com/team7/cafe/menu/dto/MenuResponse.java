package com.team7.cafe.menu.dto;

import com.team7.cafe.menu.entity.Menu;

public class MenuResponse {

    private Long id;
    private String name;
    private String description;
    private int price;
    private boolean available;
    private Long categoryId;
    private String categoryName;

    public MenuResponse() {
    }

    public MenuResponse(Long id, String name, String description, int price, boolean available, Long categoryId, String categoryName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // [1] 순환 참조 방지 : Entity -> Response 변환 전용 매서드 [2026.11.02/안형빈]
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getPrice(),
                menu.isAvailable(),
                menu.getCategory() != null ? menu.getCategory().getId() : null,
                menu.getCategory() != null ? menu.getCategory().getName() : null
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}