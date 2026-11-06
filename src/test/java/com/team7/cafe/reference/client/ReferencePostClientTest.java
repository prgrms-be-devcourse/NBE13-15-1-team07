package com.team7.cafe.reference.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.reference.ReferenceErrorCode;
import com.team7.cafe.reference.dto.ReferencePostResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// 외부 게시글 클라이언트 테스트 : 실제 RestClient가 모의 외부 서버(JDK HttpServer)를 호출한다. 외부 실서비스는 호출하지 않는다 [2026.11.06/안형빈]
// - Boot의 RestClient 자동 설정만 불러와서 DB 없이 돈다 (실제 앱과 같은 방식으로 spring.http.clients.* 타임아웃이 적용되는지도 함께 확인)
// - 타임아웃은 테스트에서만 짧게(연결 0.3초, 읽기 0.5초) 덮어써서 5초를 기다리지 않는다
@SpringJUnitConfig
@ImportAutoConfiguration({HttpClientAutoConfiguration.class, ImperativeHttpClientAutoConfiguration.class, RestClientAutoConfiguration.class})
@TestPropertySource(properties = {
        "spring.http.clients.connect-timeout=300ms",
        "spring.http.clients.read-timeout=500ms"
})
class ReferencePostClientTest {

    @Configuration
    static class EmptyConfig {
    }

    // Boot가 만든 빌더 (프로토타입 빈): 시나리오마다 복제해서 서로 영향이 없게 한다
    @Autowired
    private RestClient.Builder builder;

    private static HttpServer server;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());   // 느린 응답이 다음 요청을 막지 않게
        server.createContext("/ok/posts/7", ex -> respond(ex, 200, "{\"userId\":3,\"id\":7,\"title\":\"모의 제목\",\"body\":\"모의 본문\"}"));
        server.createContext("/notfound/posts/1", ex -> respond(ex, 404, "{}"));
        server.createContext("/error500/posts/1", ex -> respond(ex, 500, "{}"));
        server.createContext("/error503/posts/1", ex -> respond(ex, 503, "{}"));
        server.createContext("/toomany/posts/1", ex -> respond(ex, 429, "{}"));
        server.createContext("/empty/posts/1", ex -> { ex.sendResponseHeaders(200, -1); ex.close(); });
        server.createContext("/slow/posts/1", ex -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
            respond(ex, 200, "{\"userId\":1,\"id\":1,\"title\":\"t\",\"body\":\"b\"}");
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void respond(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private ReferencePostClient clientFor(String baseUrl) {
        return new ReferencePostClient(builder.clone(), baseUrl);
    }

    private static void assertApiError(Runnable call, ReferenceErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.errorCode()).isEqualTo(expected));
    }

    // [1] 성공: 외부가 준 값을 그대로 돌려준다 (고정값이 아님을 확인하려고 요청한 id와 서버가 준 값이 다른 필드를 쓴다) [2026.11.06/안형빈]
    @Test
    @DisplayName("외부가 200이면 외부 응답의 id·userId·title·body를 그대로 돌려준다")
    void readPost_success_returnsExternalValues() {
        ReferencePostResponse result = clientFor(base + "/ok").readPost(7);

        assertThat(result.id()).isEqualTo(7);
        assertThat(result.userId()).isEqualTo(3);
        assertThat(result.title()).isEqualTo("모의 제목");
        assertThat(result.body()).isEqualTo("모의 본문");
    }

    // [2] 외부 404 -> 404 REFERENCE_POST_NOT_FOUND [2026.11.06/안형빈]
    @Test
    @DisplayName("외부가 404이면 REFERENCE_POST_NOT_FOUND(404)")
    void readPost_external404_throwsNotFound() {
        assertApiError(() -> clientFor(base + "/notfound").readPost(1), ReferenceErrorCode.REFERENCE_POST_NOT_FOUND);
        assertThat(ReferenceErrorCode.REFERENCE_POST_NOT_FOUND.status().value()).isEqualTo(404);
    }

    // [3] 외부 5xx -> 502 OUTSIDE_API_FAILED, 정의되지 않은 4xx(429)도 같은 502 [2026.11.06/안형빈]
    @Test
    @DisplayName("외부가 500·503이면 OUTSIDE_API_FAILED(502)")
    void readPost_external5xx_throwsFailed() {
        assertApiError(() -> clientFor(base + "/error500").readPost(1), ReferenceErrorCode.OUTSIDE_API_FAILED);
        assertApiError(() -> clientFor(base + "/error503").readPost(1), ReferenceErrorCode.OUTSIDE_API_FAILED);
        assertThat(ReferenceErrorCode.OUTSIDE_API_FAILED.status().value()).isEqualTo(502);
    }

    @Test
    @DisplayName("외부가 404가 아닌 4xx(429)이어도 OUTSIDE_API_FAILED(502)")
    void readPost_externalOther4xx_throwsFailed() {
        assertApiError(() -> clientFor(base + "/toomany").readPost(1), ReferenceErrorCode.OUTSIDE_API_FAILED);
    }

    @Test
    @DisplayName("외부가 200인데 본문이 비어 있으면 OUTSIDE_API_FAILED(502)")
    void readPost_emptyBody_throwsFailed() {
        assertApiError(() -> clientFor(base + "/empty").readPost(1), ReferenceErrorCode.OUTSIDE_API_FAILED);
    }

    // [4] 연결 거절 -> 502 (닫힌 포트: 열었다가 닫아서 확실히 아무도 듣지 않는 포트를 얻는다) [2026.11.06/안형빈]
    @Test
    @DisplayName("연결이 거절되면 OUTSIDE_API_FAILED(502)")
    void readPost_connectionRefused_throwsFailed() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        assertApiError(() -> clientFor("http://127.0.0.1:" + closedPort).readPost(1), ReferenceErrorCode.OUTSIDE_API_FAILED);
    }

    // [5] 도메인 오류 -> 502 (.invalid는 예약된 최상위 도메인이라 절대 해석되지 않는다, 단 DNS를 가로채는 네트워크에서는 건너뜀) [2026.11.06/안형빈]
    @Test
    @DisplayName("도메인을 찾을 수 없으면 OUTSIDE_API_FAILED(502)")
    void readPost_unknownHost_throwsFailed() {
        String host = "no-such-host-reference-test.invalid";
        assumeTrue(isUnresolvable(host), "이 환경의 DNS가 존재하지 않는 도메인을 해석해 버려 건너뜀");

        assertApiError(() -> clientFor("http://" + host).readPost(1), ReferenceErrorCode.OUTSIDE_API_FAILED);
    }

    // [6] 읽기 시간 초과 -> 504 (서버가 1.5초 늦게 응답, 읽기 제한은 0.5초) [2026.11.06/안형빈]
    @Test
    @DisplayName("응답 읽기가 시간 초과되면 OUTSIDE_API_TIMEOUT(504)")
    void readPost_readTimeout_throwsTimeout() {
        long start = System.currentTimeMillis();

        assertApiError(() -> clientFor(base + "/slow").readPost(1), ReferenceErrorCode.OUTSIDE_API_TIMEOUT);

        // 서버가 응답하는 1.5초를 기다리지 않고 제한 시간(0.5초) 근처에서 끊겼는지 = 설정이 실제로 적용됐는지
        assertThat(System.currentTimeMillis() - start).isLessThan(1400);
        assertThat(ReferenceErrorCode.OUTSIDE_API_TIMEOUT.status().value()).isEqualTo(504);
    }

    // [7] 연결 시간 초과 -> 504 (라우팅되지 않는 주소로 연결, 제한 0.3초) [2026.11.06/안형빈]
    // - 이 시험은 네트워크 환경에 따라 달라진다: 어떤 네트워크는 시간 초과가 아니라 즉시 실패(network unreachable)를 돌려준다.
    //   그래서 먼저 이 환경에서 시간 초과가 실제로 나는지 확인하고, 아니면 불안정한 시험이 되지 않도록 건너뛴다.
    @Test
    @DisplayName("연결이 시간 초과되면 OUTSIDE_API_TIMEOUT(504)")
    void readPost_connectTimeout_throwsTimeout() {
        assumeTrue(canReproduceConnectTimeout(), "이 환경에서는 연결 시간 초과를 재현할 수 없어 건너뜀");

        assertApiError(() -> clientFor("http://10.255.255.1:81").readPost(1), ReferenceErrorCode.OUTSIDE_API_TIMEOUT);
    }

    private static boolean isUnresolvable(String host) {
        try {
            InetAddress.getByName(host);
            return false;
        } catch (UnknownHostException e) {
            return true;
        }
    }

    private static boolean canReproduceConnectTimeout() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("10.255.255.1", 81), 300);
            return false;
        } catch (SocketTimeoutException e) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
