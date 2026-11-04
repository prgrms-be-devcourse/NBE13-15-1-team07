package com.team7.cafe.order.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class ResponseTime {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private ResponseTime() {
    }

    static String format(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.format(FORMAT) + "+09:00";
    }
}
