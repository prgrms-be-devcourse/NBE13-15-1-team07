package com.team7.cafe.menu.dto;

import jakarta.validation.constraints.NotNull;

public class MenuAvailabilityRequest {

    @NotNull(message = "판매 상태는 필수입니다.")
    private Boolean available;

    public MenuAvailabilityRequest() {
    }

    public MenuAvailabilityRequest(Boolean available) {
        this.available = available;
    }

    public Boolean getAvailable() {
        return available;
    }
}