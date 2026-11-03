package com.team7.cafe.delivery.service;

import static com.team7.cafe.delivery.service.Rows.placed;
import static com.team7.cafe.delivery.service.Rows.withAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.team7.cafe.delivery.dto.DeliveryBatchResponse;
import com.team7.cafe.delivery.dto.DeliveryItemResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository.OrderItemRow;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** 리포지토리는 SQL로 PLACED만 읽어 오므로, 여기서는 받은 행을 묶고 합산하는 규칙만 검증한다. */
@DisplayName("A14 배송 묶음 서비스")
class DeliveryServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);
    private static final String DATE_TEXT = "2026-11-03";

    private final DeliveryQueryRepository repository = mock(DeliveryQueryRepository.class);
    private final DeliveryService service = new DeliveryService(repository);

    private final Logger serviceLogger = (Logger) LoggerFactory.getLogger(DeliveryService.class);
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void captureLogs() {
        logs = new ListAppender<>();
        logs.start();
        serviceLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        serviceLogger.detachAppender(logs);
    }

    private void givenRows(OrderItemRow... rows) {
        when(repository.findPlacedByShippingDate(DATE)).thenReturn(List.of(rows));
    }

    private List<ILoggingEvent> warnings() {
        return logs.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
    }

    @Test
    @DisplayName("주문이 없으면 빈 목록이다")
    void noOrders() {
        // given: 그날 배송할 주문이 없다 (가짜 리포지토리의 기본 응답은 빈 목록)

        // when
        List<DeliveryBatchResponse> result = service.findBatches(DATE_TEXT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 이메일의 주문은 하나로 묶고 orderIds는 오름차순·중복 없이 돌려준다")
    void groupsSameEmailWithSortedDistinctOrderIds() {
        // given: 2번 주문이 먼저 오고, 1번 주문은 항목이 2줄이다
        givenRows(
                placed(2, "a@x.com", 1, "Columbia", 5000, 1),
                placed(1, "a@x.com", 1, "Columbia", 5000, 3),
                placed(1, "a@x.com", 2, "Brazil", 7000, 1));

        // when
        List<DeliveryBatchResponse> result = service.findBatches(DATE_TEXT);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderIds()).containsExactly(1L, 2L);
        assertThat(result.get(0).orderCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("묶음의 항목은 합산하고 총 수량·총 금액을 계산한다")
    void sumsItemsAndTotals() {
        // given: 두 주문에 메뉴 1이 걸쳐 있다
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 3),
                placed(1, "a@x.com", 2, "Brazil", 7000, 1),
                placed(2, "a@x.com", 1, "Columbia", 5000, 1));

        // when
        List<DeliveryBatchResponse> result = service.findBatches(DATE_TEXT);

        // then
        assertThat(result).containsExactly(new DeliveryBatchResponse(
                DATE, "a@x.com", "서울시 실습로 10", "01234", List.of(1L, 2L), 2,
                List.of(new DeliveryItemResponse(1, "Columbia", 5000, 4, 20000),
                        new DeliveryItemResponse(2, "Brazil", 7000, 1, 7000)),
                27000, 5));
    }

    @Test
    @DisplayName("항목은 메뉴 ID·이름·단가가 모두 같을 때만 합산하고 메뉴 ID → 단가 → 이름 순으로 정렬한다")
    void itemKeyAndOrdering() {
        // given: 이름의 대소문자가 다른 항목과 단가가 다른 항목이 뒤섞여 있다
        givenRows(
                placed(1, "a@x.com", 2, "Brazil", 7500, 1),
                placed(1, "a@x.com", 1, "columbia", 5000, 2),
                placed(2, "a@x.com", 1, "Columbia", 5000, 1),
                placed(2, "a@x.com", 2, "Brazil", 7000, 1));

        // when
        List<DeliveryItemResponse> items = service.findBatches(DATE_TEXT).get(0).items();

        // then
        assertThat(items).containsExactly(
                new DeliveryItemResponse(1, "Columbia", 5000, 1, 5000),
                new DeliveryItemResponse(1, "columbia", 5000, 2, 10000),
                new DeliveryItemResponse(2, "Brazil", 7000, 1, 7000),
                new DeliveryItemResponse(2, "Brazil", 7500, 1, 7500));
    }

    @Test
    @DisplayName("이메일이 다르면 별도 묶음이고 이메일 오름차순이며 대소문자를 구분한다")
    void separateBatchesSortedByEmail() {
        // given: 입력 순서는 b, a, A로 뒤섞여 있다
        givenRows(
                placed(1, "b@x.com", 1, "Columbia", 5000, 1),
                placed(2, "a@x.com", 1, "Columbia", 5000, 1),
                placed(3, "A@x.com", 1, "Columbia", 5000, 1));

        // when
        List<DeliveryBatchResponse> result = service.findBatches(DATE_TEXT);

        // then
        assertThat(result)
                .extracting(DeliveryBatchResponse::email)
                .containsExactly("A@x.com", "a@x.com", "b@x.com");
    }

    @Test
    @DisplayName("묶음의 주소와 우편번호, 배송일을 응답에 담는다")
    void carriesAddressAndShippingDate() {
        // given: 부산 주소의 주문 1건
        givenRows(withAddress(placed(1, "a@x.com", 1, "Columbia", 5000, 1), "부산시 해운대로 1", "48000"));

        // when
        DeliveryBatchResponse batch = service.findBatches(DATE_TEXT).get(0);

        // then
        assertThat(batch.shippingDate()).isEqualTo(DATE);
        assertThat(batch.address()).isEqualTo("부산시 해운대로 1");
        assertThat(batch.postcode()).isEqualTo("48000");
    }

    @Test
    @DisplayName("묶음 안 주소가 어긋나도 조회는 실패하지 않고 첫 주문의 주소를 쓰며 경고 로그를 남긴다")
    void addressMismatchWarnsButDoesNotFail() {
        // given: 2번 주문의 주소·우편번호가 1번 주문과 다르다
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 1),
                withAddress(placed(2, "a@x.com", 1, "Columbia", 5000, 1), "다른 주소 7", "05555"));

        // when
        List<DeliveryBatchResponse> result = service.findBatches(DATE_TEXT);

        // then: 조회는 성공하고, 주소는 첫 주문의 값이며, 경고가 1번 남는다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).address()).isEqualTo("서울시 실습로 10");
        assertThat(result.get(0).postcode()).isEqualTo("01234");
        assertThat(result.get(0).orderIds()).containsExactly(1L, 2L);
        assertThat(warnings()).hasSize(1);
        assertThat(warnings().get(0).getFormattedMessage()).contains("주소 불일치", "[2]");
    }

    @Test
    @DisplayName("앞뒤 공백만 다른 주소는 같은 주소로 보고 경고하지 않는다")
    void whitespaceOnlyDifferenceDoesNotWarn() {
        // given: 2번 주문의 주소 앞뒤에 공백이 붙어 있다
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 1),
                withAddress(placed(2, "a@x.com", 1, "Columbia", 5000, 1), "  서울시 실습로 10  ", "01234"));

        // when
        List<DeliveryBatchResponse> result = service.findBatches(DATE_TEXT);

        // then
        assertThat(result).hasSize(1);
        assertThat(warnings()).isEmpty();
    }

    @Test
    @DisplayName("올바르지 않은 배송일은 VALIDATION_ERROR이고 DB를 조회하지 않는다")
    void invalidDate() {
        // given: 존재하지 않는 날짜

        // when & then
        assertThatThrownBy(() -> service.findBatches("2026-02-30")).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
            assertThat(e.fieldErrors()).containsKey("shippingDate");
        });
        verifyNoInteractions(repository);
    }
}
