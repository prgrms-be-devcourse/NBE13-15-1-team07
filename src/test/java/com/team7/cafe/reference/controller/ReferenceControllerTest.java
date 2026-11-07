package com.team7.cafe.reference.controller;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.reference.ReferenceErrorCode;
import com.team7.cafe.reference.client.ReferencePostClient;
import com.team7.cafe.reference.dto.ReferencePostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 외부 게시글 컨트롤러 테스트 : 모의 클라이언트를 사용하고 외부 서버·DB는 연결하지 않는다 [2026.11.06/안형빈]
@WebMvcTest(ReferenceController.class)
class ReferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferencePostClient referencePostClient;

    // [1] 정상: 클라이언트가 준 값을 id·userId·title·body 4개 필드로 200에 돌려준다 [2026.11.06/안형빈]
    @Test
    @DisplayName("외부 게시글 조회에 성공하면 200과 id·userId·title·body를 돌려준다")
    void getReferencePost_returns200WithFields() throws Exception {
        when(referencePostClient.readPost(7L)).thenReturn(new ReferencePostResponse(7, 3, "제목", "본문"));

        mockMvc.perform(get("/api/v1/reference/posts/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.title").value("제목"))
                .andExpect(jsonPath("$.body").value("본문"));

        verify(referencePostClient).readPost(7L);
    }

    // [2] 입력 오류: postId가 1 이상의 정수가 아니면 400 VALIDATION_ERROR이고 fieldErrors에 postId가 담긴다 [2026.11.06/안형빈]
    //     (0, 음수, 숫자가 아님, 소수, long 범위 초과) - 외부 호출은 일어나지 않는다
    @ParameterizedTest(name = "postId=\"{0}\"이면 400")
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "99999999999999999999"})
    @DisplayName("postId가 1 이상의 정수가 아니면 400, VALIDATION_ERROR, fieldErrors에 postId")
    void getReferencePost_invalidPostId_returns400(String postId) throws Exception {
        mockMvc.perform(get("/api/v1/reference/posts/{postId}", postId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.fieldErrors.postId").exists());

        verify(referencePostClient, never()).readPost(anyLong());
    }

    // [3] 외부 호출 실패: 클라이언트가 던진 ApiException이 상태 코드·code·message·빈 fieldErrors 그대로 응답된다 [2026.11.06/안형빈]
    //     (404 REFERENCE_POST_NOT_FOUND, 502 OUTSIDE_API_FAILED, 504 OUTSIDE_API_TIMEOUT)
    @ParameterizedTest(name = "{0}")
    @EnumSource(ReferenceErrorCode.class)
    @DisplayName("클라이언트 실패는 오류 코드의 상태·code·message와 빈 fieldErrors로 응답된다")
    void getReferencePost_clientFailure_returnsMappedError(ReferenceErrorCode errorCode) throws Exception {
        when(referencePostClient.readPost(1L)).thenThrow(new ApiException(errorCode));

        mockMvc.perform(get("/api/v1/reference/posts/1"))
                .andExpect(status().is(errorCode.status().value()))
                .andExpect(jsonPath("$.code").value(errorCode.code()))
                .andExpect(jsonPath("$.message").value(errorCode.message()))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
}
