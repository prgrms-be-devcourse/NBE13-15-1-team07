package com.team7.cafe.reference.client;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.reference.ReferenceErrorCode;
import com.team7.cafe.reference.dto.ReferencePostResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

// [1] 심화: 외부 서버(JSONPlaceholder) 호출 클라이언트 [2026.11.06/안형빈]
// * 주의: RestClient.Builder 빈을 직접 만들지 않고, builder.requestFactory(...)도 호출하지 않는다 (spring.http.clients.* 타임아웃이 무시됨)
@Component
public class ReferencePostClient {
    private final RestClient restClient;

    // [2] 생성자: Boot가 만든 RestClient.Builder에 외부 주소만 붙여 RestClient를 만든다 (기본 주소는 설정으로 바꿀 수 있음) [2026.11.06/안형빈]
    @Autowired
    public ReferencePostClient(
            RestClient.Builder builder,
            @Value("${app.outside.base-url:https://jsonplaceholder.typicode.com}")
            String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    // [3] 외부 게시글 조회: 외부 서버의 /posts/{postId}를 호출하고, 실패는 ApiException으로 바꿔 던진다 [2026.11.06/안형빈]
    public ReferencePostResponse readPost(long postId) {
        ReferencePostResponse found;
        try {
            found = this.restClient
                    .get()
                    .uri("/posts/{postId}", postId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ReferencePostResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            // 외부가 404: 게시글이 없음 -> 404
            throw new ApiException(ReferenceErrorCode.REFERENCE_POST_NOT_FOUND);
        } catch (HttpStatusCodeException e) {
            // 그 외 4xx, 5xx 전부 -> 502
            throw new ApiException(ReferenceErrorCode.OUTSIDE_API_FAILED);
        } catch (ResourceAccessException e) {
            // 연결 계층 오류: 시간 초과면 504, 연결 거절·도메인 오류면 502
            throw new ApiException(isTimeout(e) ? ReferenceErrorCode.OUTSIDE_API_TIMEOUT : ReferenceErrorCode.OUTSIDE_API_FAILED);
        } catch (RestClientException e) {
            // 그 외 응답 변환 실패 등 -> 502
            throw new ApiException(ReferenceErrorCode.OUTSIDE_API_FAILED);
        }

        // 200인데 본문이 비어 있는 경우 -> 502
        if (found == null) {
            throw new ApiException(ReferenceErrorCode.OUTSIDE_API_FAILED);
        }
        return found;
    }

    // [4] 시간 초과 판별: 원인 사슬에 HttpTimeoutException(연결 시간 초과 포함) 또는 SocketTimeoutException이 있는지 본다 [2026.11.06/안형빈]
    // - 시간 초과를 먼저 검사해야 한다 (연결 시간 초과의 원인 사슬 안에도 ConnectException이 들어 있어 순서를 바꾸면 504가 502로 나감)
    private boolean isTimeout(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
            Throwable next = cause.getCause();
            if (next == cause) {
                break;
            }
            cause = next;
        }
        return false;
    }
}
