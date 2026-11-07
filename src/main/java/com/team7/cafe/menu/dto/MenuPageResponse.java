package com.team7.cafe.menu.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// [1] 메뉴 페이징 응답 : content, number, size, totalElements, totalPages 5개 필드 [2026.11.06/안형빈]
public record MenuPageResponse(
        List<MenuResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages
){

    // [2] Page -> 응답 변환 : 범위를 넘은 페이지는 content가 빈 목록이고 전체 건수·페이지 수는 그대로 [2026.11.06/안형빈]
    public static MenuPageResponse from (Page<MenuResponse> page){
        return new MenuPageResponse(
                page.getContent(),          // 1. 실제 데이터 목록
                page.getNumber(),           // 2. 현재 페이지 번호 (0부터 시작)
                page.getSize(),             // 3. 페이지당 크기
                page.getTotalElements(),    // 4. 총 건수
                page.getTotalPages()        // 5. 총 페이지 수
        );
    }
}