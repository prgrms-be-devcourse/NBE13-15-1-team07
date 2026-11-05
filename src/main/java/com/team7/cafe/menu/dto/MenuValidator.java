package com.team7.cafe.menu.dto;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

import java.util.Map;

// 메뉴 입력 검증 공통 규칙 : 오류는 첫 번째에서 끊지 않고 errors에 모은 뒤 throwIfAny로 한 번에 던진다 [2026.11.03/안형빈]
// Jackson이 "5000" 같은 문자열을 int로 자동 변환하지 못하도록 요청 본문은 Map으로 받아 타입을 직접 검사한다 [2026.11.03/안형빈]
public final class MenuValidator {

    private MenuValidator() {
    }

    // [1] 앞뒤 공백 제거 : 공백·탭·줄바꿈만, 중간 공백은 유지 [2026.11.03/안형빈]
    static String trim(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isBlankChar(s.charAt(start))) {
            start++;
        }
        while (end > start && isBlankChar(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(start, end);
    }

    private static boolean isBlankChar(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    // [2] 정수 판별 : Integer/Long만 허용, 문자열·소수·null은 거절 [2026.11.03/안형빈]
    static Long asInteger(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        return null;
    }

    // [3] 경로 ID 검증 : 1 이상의 정수 [2026.11.03/안형빈]
    public static Long id(String raw, String field, Map<String, String> errors) {
        if (raw != null && raw.matches("[0-9]+")) {
            try {
                long value = Long.parseLong(raw);
                if (value >= 1) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // 범위 초과는 아래에서 오류로 처리 [2026.11.03/안형빈]
            }
        }
        errors.putIfAbsent(field, "1 이상의 정수여야 합니다.");
        return null;
    }

    // [4] A20 검색어 검증 : trim 후 1~100자 [2026.11.03/안형빈]
    public static String keyword(String raw, Map<String, String> errors) {
        String trimmed = (raw == null) ? "" : trim(raw);
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            errors.putIfAbsent("keyword", "검색어는 공백 제거 후 1~100자여야 합니다.");
            return null;
        }
        return trimmed;
    }

    // [5] 모인 오류가 있으면 VALIDATION_ERROR(400)로 던짐 [2026.11.03/안형빈]
    public static void throwIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ApiException(CommonErrorCode.VALIDATION_ERROR, errors);
        }
    }
}
