package com.team7.cafe.menu.dto;

import java.util.Map;

public class MenuRequest {

    private final String name;
    private final String description;
    private final int price;
    private final long categoryId;

    public MenuRequest(String name, String description, int price, long categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
    }

    // [1] A08, A09 본문 검증 : 위반된 필드는 모두 errors에 담고, 문서에 없는 필드는 무시 [2026.11.03/안형빈]
    public static MenuRequest parse(Map<String, Object> body, Map<String, String> errors) {
        Long categoryId = MenuValidator.asInteger(body.get("categoryId"));
        if (categoryId == null || categoryId < 1) {
            errors.put("categoryId", "카테고리 ID는 1 이상의 정수여야 합니다.");
        }

        String name = null;
        if (body.get("name") instanceof String raw) {
            String trimmed = MenuValidator.trim(raw);
            if (!trimmed.isEmpty() && trimmed.length() <= 100) {
                name = trimmed;
            }
        }
        if (name == null) {
            errors.put("name", "메뉴명은 공백 제거 후 1~100자여야 합니다.");
        }

        Long price = MenuValidator.asInteger(body.get("price"));
        if (price == null || price < 1 || price > 1_000_000) {
            errors.put("price", "가격은 1~1,000,000 사이의 정수여야 합니다.");
        }

        String description = null;
        if (body.get("description") instanceof String raw) {
            String trimmed = MenuValidator.trim(raw);
            if (trimmed.length() <= 300) {
                description = trimmed;
            }
        }
        if (description == null) {
            errors.put("description", "설명은 필수이며 공백 제거 후 0~300자여야 합니다.");
        }

        if (!errors.isEmpty()) {
            return null;
        }
        return new MenuRequest(name, description, price.intValue(), categoryId);
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

    public Long getCategoryId() {
        return categoryId;
    }
}
