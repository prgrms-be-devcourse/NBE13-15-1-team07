package com.team7.cafe.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.team7.cafe.delivery.dto.CustomerStatisticsResponse;
import com.team7.cafe.delivery.dto.PageResponse;
import com.team7.cafe.delivery.repository.CustomerStatRow;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;

@DisplayName("A24 접수일별 고객 통계 페이징 서비스")
class CustomerStatisticsPageServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);
    private static final String DATE_TEXT = "2026-11-03";

    private final DeliveryQueryRepository repository = mock(DeliveryQueryRepository.class);
    private final StatisticsService service = new StatisticsService(repository);

    private void givenPage(Pageable pageable, long total, CustomerStatRow... rows) {
        when(repository.findCustomerStatsByReceivedDate(eq(DATE), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(rows), pageable, total));
    }

    @Test
    @DisplayName("DB가 계산한 한 줄을 응답으로 옮기고, 취소 건수는 전체 − 접수로 구한다")
    void mapsRowsAndPageInfo() {
        // given: 한 페이지 크기 2, 전체 5명 중 첫 페이지
        givenPage(PageRequest.of(0, 2), 5,
                new CustomerStatRow("a@x.com", 3, 2, 5, 27000),
                new CustomerStatRow("b@x.com", 1, 0, 0, 0));

        // when
        PageResponse<CustomerStatisticsResponse> result = service.customerStatisticsPage(DATE_TEXT, "0", "2");

        // then
        assertThat(result.content()).containsExactly(
                new CustomerStatisticsResponse(DATE, "a@x.com", 3, 2, 1, 5, 27000),
                new CustomerStatisticsResponse(DATE, "b@x.com", 1, 0, 1, 0, 0));
        assertThat(result.number()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("page·size를 생략하면 page 0, size 10으로 DB에 요청한다")
    void defaults() {
        // given
        when(repository.findCustomerStatsByReceivedDate(eq(DATE), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        // when
        service.customerStatisticsPage(DATE_TEXT, null, null);

        // then: 서비스가 넘긴 페이지 요청이 0 / 10이다
        verify(repository).findCustomerStatsByReceivedDate(DATE, PageRequest.of(0, 10));
    }

    @ParameterizedTest(name = "size={0}은 허용")
    @ValueSource(strings = {"1", "50"})
    @DisplayName("size 경계 1과 50은 허용한다")
    void sizeBoundaries(String size) {
        // given
        when(repository.findCustomerStatsByReceivedDate(eq(DATE), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, Integer.parseInt(size))));

        // when
        service.customerStatisticsPage(DATE_TEXT, "0", size);

        // then
        verify(repository).findCustomerStatsByReceivedDate(DATE, PageRequest.of(0, Integer.parseInt(size)));
    }

    @ParameterizedTest(name = "size={0}은 400")
    @ValueSource(strings = {"0", "51", "abc"})
    @DisplayName("size가 1~50 정수가 아니면 400이고 DB를 읽지 않는다")
    void invalidSize(String size) {
        // given: 잘못된 size

        // when & then
        assertThatThrownBy(() -> service.customerStatisticsPage(DATE_TEXT, "0", size))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
                    assertThat(e.fieldErrors()).containsOnlyKeys("size");
                });
        verifyNoInteractions(repository);
    }

    @ParameterizedTest(name = "page={0}은 400")
    @ValueSource(strings = {"-1", "abc"})
    @DisplayName("page가 0 이상 정수가 아니면 400이고 DB를 읽지 않는다")
    void invalidPage(String page) {
        // given: 잘못된 page

        // when & then
        assertThatThrownBy(() -> service.customerStatisticsPage(DATE_TEXT, page, "10"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
                    assertThat(e.fieldErrors()).containsOnlyKeys("page");
                });
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("page와 size가 모두 틀리면 두 필드 오류를 한 번에 알려 준다")
    void bothInvalid() {
        // given: 둘 다 잘못된 값

        // when & then
        assertThatThrownBy(() -> service.customerStatisticsPage(DATE_TEXT, "-1", "0"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.fieldErrors()).containsOnlyKeys("page", "size"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("날짜가 틀리면 400이고 DB를 읽지 않는다")
    void invalidDate() {
        // given: 존재하지 않는 날짜

        // when & then
        assertThatThrownBy(() -> service.customerStatisticsPage("2026-02-30", "0", "10"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(CommonErrorCode.VALIDATION_ERROR);
                    assertThat(e.fieldErrors()).containsKey("date");
                });
        verifyNoInteractions(repository);
    }
}
