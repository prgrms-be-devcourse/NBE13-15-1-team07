package com.team7.cafe.reference.dto;

// [1] 심화: 외부 게시글 응답, id·userId는 정수 title·body는 문자열 (외부 서버가 준 값을 그대로 사용, 고정값 반환 금지) [2026.11.06/안형빈]
public record ReferencePostResponse(int id, int userId, String title, String body) {
}
