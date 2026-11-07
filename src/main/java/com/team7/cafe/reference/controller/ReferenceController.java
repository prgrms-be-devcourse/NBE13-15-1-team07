package com.team7.cafe.reference.controller;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;
import com.team7.cafe.reference.client.ReferencePostClient;
import com.team7.cafe.reference.dto.ReferencePostResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 입력 검증(400)은 컨트롤러에서 끝내고, 외부 호출 실패(404·502·504)는 클라이언트가 ApiException으로 던진다 [2026.11.06/안형빈]
// postId는 String으로 받아 직접 파싱해야 fieldErrors의 키 이름(postId)을 통제할 수 있다 (MenuController의 menuId와 같은 이유) [2026.11.06/안형빈]

@RestController
@RequestMapping("/api/v1")
public class ReferenceController {

    private final ReferencePostClient referencePostClient;

    @Autowired
    public ReferenceController(ReferencePostClient referencePostClient) {
        this.referencePostClient = referencePostClient;
    }

    // [1] 심화: 외부 게시글 조회 (GET /api/v1/reference/posts/{postId}), 검사 순서는 400 -> 404 -> 502/504 [2026.11.06/안형빈]
    @GetMapping ("/reference/posts/{postId}")
    public ResponseEntity<ReferencePostResponse> getReferencePost(
            @PathVariable("postId") String postId
    ){
        // 형식 오류면 여기서 400으로 끝난다
        long id = parsePostId(postId);
        // 외부 호출 결과를 200으로 돌려준다
        return ResponseEntity.ok(referencePostClient.readPost(id));
    }

    // [2] postId 검증: 숫자 문자만 허용하고 1 이상의 long이면 통과, 그 외(0, 음수, 숫자가 아님, 범위 초과)는 400 VALIDATION_ERROR [2026.11.06/안형빈]
    private long parsePostId(String postId){
        if (postId != null && postId.matches("[0-9]+")){
            try {
                long value = Long.parseLong(postId);
                if (value >= 1){
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // long 범위를 넘는 숫자도 아래에서 같은 400으로 처리한다
            }
        }
        throw new ApiException(CommonErrorCode.VALIDATION_ERROR, Map.of("postId", "1 이상의 정수여야 합니다."));
    }
}
