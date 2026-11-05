package com.team7.cafe.menu.dto;

import java.util.Map;

public class MenuAvailabilityRequest {

    private final boolean available;

    public MenuAvailabilityRequest(boolean available) {
        this.available = available;
    }

    // [1] A21 본문 검증 : JSON boolean만 허용, 문자열·null·누락은 거절 [2026.11.03/안형빈]
    public static MenuAvailabilityRequest parse(Map<String, Object> body, Map<String, String> errors) {
        if (body.get("available") instanceof Boolean value) {
            return new MenuAvailabilityRequest(value);
        }
        errors.put("available", "판매 여부는 true 또는 false여야 합니다.");
        return null;
    }

    public boolean getAvailable() {
        return available;
    }
}
