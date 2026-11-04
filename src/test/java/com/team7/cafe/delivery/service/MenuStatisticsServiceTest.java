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

import com.team7.cafe.delivery.dto.MenuStatisticsResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.OrderItemRow;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

@DisplayName("A18 접수일별 메뉴 통계 서비스")
class MenuStatisticsServiceTest {

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
        List<MenuStatisticsResponse> result = service.menuStatistics(DATE_TEXT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("PLACED만 합산하고, 취소된 주문에만 있던 메뉴는 목록에서 제외한다")
    void excludesMenusOnlyInCancelledOrders() {
        // given: 메뉴 1은 접수와 취소에 모두 있고, 메뉴 3은 취소 주문에만 있다
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 3),
                cancelled(2, "b@x.com", 1, "Columbia", 5000, 4),
                cancelled(2, "b@x.com", 3, "Quindio", 9000, 2));

        // when
        List<MenuStatisticsResponse> result = service.menuStatistics(DATE_TEXT);

        // then
        assertThat(result).containsExactly(new MenuStatisticsResponse(1, "Columbia", 5000, 3, 15000));
    }

    @Test
    @DisplayName("메뉴 ID·이름·단가가 같으면 주문이 달라도 합산한다")
    void sumsSameItemAcrossOrders() {
        // given: 서로 다른 두 주문에 같은 메뉴가 있다
        givenRows(
                placed(1, "a@x.com", 1, "Columbia", 5000, 3),
                placed(2, "b@x.com", 1, "Columbia", 5000, 2));

        // when
        List<MenuStatisticsResponse> result = service.menuStatistics(DATE_TEXT);

        // then
        assertThat(result).containsExactly(new MenuStatisticsResponse(1, "Columbia", 5000, 5, 25000));
    }

    @Test
    @DisplayName("메뉴 이름이 대소문자만 달라도 합산하지 않고 대문자가 먼저 나온다")
    void nameIsCaseSensitive() {
        // given: 메뉴 ID와 단가는 같고 이름의 대소문자만 다르다
        givenRows(
                placed(1, "a@x.com", 1, "columbia", 5000, 2),
                placed(2, "b@x.com", 1, "Columbia", 5000, 1));

        // when
        List<MenuStatisticsResponse> result = service.menuStatistics(DATE_TEXT);

        // then
        assertThat(result).containsExactly(
                new MenuStatisticsResponse(1, "Columbia", 5000, 1, 5000),
                new MenuStatisticsResponse(1, "columbia", 5000, 2, 10000));
    }

    @Test
    @DisplayName("단가가 다르면 합산하지 않고 메뉴 ID → 단가 오름차순으로 정렬한다")
    void priceDiffersAndOrdering() {
        // given: 메뉴 2의 단가가 7500과 7000으로 다르고, 입력 순서는 뒤섞여 있다
        givenRows(
                placed(1, "a@x.com", 2, "Brazil", 7500, 1),
                placed(2, "b@x.com", 2, "Brazil", 7000, 1),
                placed(3, "c@x.com", 1, "Columbia", 5000, 1));

        // when
        List<MenuStatisticsResponse> result = service.menuStatistics(DATE_TEXT);

        // then
        assertThat(result).containsExactly(
                new MenuStatisticsResponse(1, "Columbia", 5000, 1, 5000),
                new MenuStatisticsResponse(2, "Brazil", 7000, 1, 7000),
                new MenuStatisticsResponse(2, "Brazil", 7500, 1, 7500));
    }

    @Test
    @DisplayName("올바르지 않은 날짜는 VALIDATION_ERROR이고 DB를 조회하지 않는다")
    void invalidDate() {
        // given: 존재하지 않는 날짜

        // when & then
        assertThatThrownBy(() -> service.menuStatistics("2026-02-30")).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
            assertThat(e.fieldErrors()).containsKey("date");
        });
        verifyNoInteractions(repository);
    }
}
