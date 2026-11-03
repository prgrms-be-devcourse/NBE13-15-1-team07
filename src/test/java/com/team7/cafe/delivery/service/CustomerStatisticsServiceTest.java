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

import com.team7.cafe.delivery.dto.CustomerStatisticsResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository.OrderItemRow;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

@DisplayName("A24 접수일별 고객 통계 서비스")
class CustomerStatisticsServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);
    private static final String DATE_TEXT = "2026-11-03";

    private final DeliveryQueryRepository repository = mock(DeliveryQueryRepository.class);
    private final StatisticsService service = new StatisticsService(repository);

    private void givenRows(OrderItemRow... rows) {
        when(repository.findByReceivedDate(DATE)).thenReturn(List.of(rows));
    }

    @Test
    @DisplayName("주문이 없으면 빈 목록이다")
    void noOrders() {
        // given: 그날 접수된 주문이 없다 (가짜 리포지토리의 기본 응답은 빈 목록)

        // when
        List<CustomerStatisticsResponse> result = service.customerStatistics(DATE_TEXT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이메일 오름차순이며 대소문자를 구분한다")
    void sortedByEmailCaseSensitive() {
        // given: 입력 순서는 b, a, A로 뒤섞여 있다
        givenRows(
                placed(1, "b@x.com", 1, "Columbia", 5000, 1),
                placed(2, "a@x.com", 1, "Columbia", 5000, 1),
                placed(3, "A@x.com", 1, "Columbia", 5000, 1));

        // when
        List<CustomerStatisticsResponse> result = service.customerStatistics(DATE_TEXT);

        // then: 대문자 A가 먼저이고, A@x.com과 a@x.com은 다른 고객이다
        assertThat(result)
                .extracting(CustomerStatisticsResponse::email)
                .containsExactly("A@x.com", "a@x.com", "b@x.com");
    }

    @Test
    @DisplayName("모두 취소한 고객도 포함하며 PLACED 수·수량·금액은 0이다")
    void includesFullyCancelledCustomer() {
        // given: b@x.com은 취소 주문만 있다
        givenRows(cancelled(1, "b@x.com", 3, "Quindio", 9000, 2));

        // when
        List<CustomerStatisticsResponse> result = service.customerStatistics(DATE_TEXT);

        // then
        assertThat(result).containsExactly(new CustomerStatisticsResponse(DATE, "b@x.com", 1, 0, 1, 0, 0));
    }

    @Test
    @DisplayName("고객의 주문 수는 취소를 포함하고 수량·금액은 PLACED만 센다")
    void mixedCustomer() {
        // given: a@x.com은 접수 2건(1번 주문은 항목 2줄)과 취소 1건이다
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 3),
                placed(1, "a@x.com", 2, "Brazil", 7000, 1),
                placed(2, "a@x.com", 1, "Columbia", 5000, 1),
                cancelled(3, "a@x.com", 1, "Columbia", 5000, 2));

        // when
        List<CustomerStatisticsResponse> result = service.customerStatistics(DATE_TEXT);

        // then
        assertThat(result).containsExactly(new CustomerStatisticsResponse(DATE, "a@x.com", 3, 2, 1, 5, 27000));
    }

    @Test
    @DisplayName("고객마다 따로 집계한다")
    void separatesCustomers() {
        // given: 서로 다른 두 고객
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 1),
                placed(2, "c@x.com", 2, "Brazil", 7000, 2));

        // when
        List<CustomerStatisticsResponse> result = service.customerStatistics(DATE_TEXT);

        // then
        assertThat(result).containsExactly(
                new CustomerStatisticsResponse(DATE, "a@x.com", 1, 1, 0, 1, 5000),
                new CustomerStatisticsResponse(DATE, "c@x.com", 1, 1, 0, 2, 14000));
    }

    @Test
    @DisplayName("올바르지 않은 날짜는 VALIDATION_ERROR이고 DB를 조회하지 않는다")
    void invalidDate() {
        // given: 존재하지 않는 날짜

        // when & then
        assertThatThrownBy(() -> service.customerStatistics("2026-02-30")).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
            assertThat(e.fieldErrors()).containsKey("date");
        });
        verifyNoInteractions(repository);
    }
}
