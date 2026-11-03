package com.team7.cafe.delivery.service;

import static com.team7.cafe.delivery.service.Rows.cancelled;
import static com.team7.cafe.delivery.service.Rows.placed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.team7.cafe.delivery.dto.OrderStatisticsResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository.OrderItemRow;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

@DisplayName("A17 접수일별 주문 통계 서비스")
class OrderStatisticsServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);
    private static final String DATE_TEXT = "2026-11-03";

    private final DeliveryQueryRepository repository = mock(DeliveryQueryRepository.class);
    private final StatisticsService service = new StatisticsService(repository);

    private void givenRows(OrderItemRow... rows) {
        when(repository.findByReceivedDate(DATE)).thenReturn(List.of(rows));
    }

    @Test
    @DisplayName("주문이 없으면 조회한 날짜와 모두 0인 객체를 돌려준다")
    void noOrders() {
        // given: 그날 접수된 주문이 없다 (가짜 리포지토리의 기본 응답은 빈 목록)

        // when
        OrderStatisticsResponse result = service.orderStatistics(DATE_TEXT);

        // then
        assertThat(result).isEqualTo(OrderStatisticsResponse.empty(DATE));
    }

    @Test
    @DisplayName("전체 주문 수는 취소를 포함하고 수량·금액은 PLACED만 센다")
    void countsAllButSumsPlacedOnly() {
        // given: 접수 2건(1번 주문은 항목 2줄)과 취소 1건
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 3),
                placed(1, "a@x.com", 2, "Brazil", 7000, 1),
                placed(2, "a@x.com", 1, "Columbia", 5000, 1),
                cancelled(3, "b@x.com", 3, "Quindio", 9000, 2));

        // when
        OrderStatisticsResponse result = service.orderStatistics(DATE_TEXT);

        // then
        assertThat(result).isEqualTo(new OrderStatisticsResponse(DATE, 3, 2, 1, 5, 27000));
    }

    @Test
    @DisplayName("취소 주문만 있으면 주문 수만 있고 수량·금액은 0이다")
    void onlyCancelled() {
        // given
        givenRows(cancelled(1, "a@x.com", 1, "Columbia", 5000, 2));

        // when
        OrderStatisticsResponse result = service.orderStatistics(DATE_TEXT);

        // then
        assertThat(result).isEqualTo(new OrderStatisticsResponse(DATE, 1, 0, 1, 0, 0));
    }

    @Test
    @DisplayName("올바르지 않은 날짜는 VALIDATION_ERROR이고 DB를 조회하지 않는다 (날짜 규칙 전체는 DateParamTest)")
    void invalidDate() {
        // given: 존재하지 않는 날짜

        // when & then
        assertThatThrownBy(() -> service.orderStatistics("2026-02-30")).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
            assertThat(e.fieldErrors()).containsKey("date");
        });
        verifyNoInteractions(repository);
    }
}
