package com.team7.cafe.delivery.dto;

import java.util.List;

/** 페이징 응답. number는 0부터 시작하는 현재 페이지 번호다. 범위를 넘긴 페이지는 content가 빈 목록이다. */
public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages) {
}
