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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.team7.cafe.delivery.dto.MenuStatisticsResponse;
import com.team7.cafe.delivery.dto.PageResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.MenuSumRow;
import com.team7.cafe.global.error.ApiException;

@DisplayName("A18 접수일별 메뉴 통계 페이징 서비스")
class MenuStatisticsPageServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    private final DeliveryQueryRepository repository = mock(DeliveryQueryRepository.class);
    private final StatisticsService service = new StatisticsService(repository);

    private MenuSumRow row(long menuId, String name, int price, long quantity, long amount) {
        MenuSumRow row = mock(MenuSumRow.class);
        when(row.getMenuId()).thenReturn(menuId);
        when(row.getMenuName()).thenReturn(name);
        when(row.getUnitPrice()).thenReturn(price);
        when(row.getTotalQuantity()).thenReturn(quantity);
        when(row.getTotalAmount()).thenReturn(amount);
        return row;
    }

    @Test
    @DisplayName("DB가 계산한 한 줄을 응답으로 옮기고 페이지 정보를 채운다")
    void mapsRowsAndPageInfo() {
        // given: 한 페이지 크기 2, 전체 5개 중 첫 페이지
        Pageable pageable = PageRequest.of(0, 2);
        List<MenuSumRow> rows = List.of(row(1, "Columbia", 5000, 3, 15000), row(2, "Brazil", 7000, 1, 7000));
        when(repository.findMenuSumsByReceivedDate(eq(DATE), eq(pageable))).thenReturn(new PageImpl<>(rows, pageable, 5));

        // when
        PageResponse<MenuStatisticsResponse> result = service.menuStatisticsPage("2026-11-03", "0", "2");

        // then
        assertThat(result.content()).containsExactly(
                new MenuStatisticsResponse(1, "Columbia", 5000, 3, 15000),
                new MenuStatisticsResponse(2, "Brazil", 7000, 1, 7000));
        assertThat(result.number()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("page·size를 생략하면 page 0, size 10으로 DB에 요청한다")
    void defaults() {
        // given
        when(repository.findMenuSumsByReceivedDate(eq(DATE), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        // when
        service.menuStatisticsPage("2026-11-03", null, null);

        // then
        verify(repository).findMenuSumsByReceivedDate(DATE, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("page·size·date가 틀리면 400이고 DB를 읽지 않는다")
    void invalidParams() {
        // given: page와 size가 모두 틀림
        // when & then
        assertThatThrownBy(() -> service.menuStatisticsPage("2026-11-03", "-1", "51"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.fieldErrors()).containsOnlyKeys("page", "size"));
        assertThatThrownBy(() -> service.menuStatisticsPage("2026-02-30", "0", "10"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.fieldErrors()).containsKey("date"));
        verifyNoInteractions(repository);
    }
}
