package com.team7.cafe.menu.dto;

import com.team7.cafe.menu.entity.Menu;

public class MenuResponse {

    private Long id;
    private Long categoryId;
    private String name;
    private int price;
    private String description;
    private boolean available;

    public MenuResponse() {
    }

    public MenuResponse(Long id, Long categoryId, String name, int price, String description, boolean available) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.available = available;
    }

    // [1] 순환 참조 방지 : Entity -> Response 변환 전용 매서드 [2026.11.02/안형빈]
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getCategory().getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getDescription(),
                menu.isAvailable()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAvailable() {
        return available;
    }
}
