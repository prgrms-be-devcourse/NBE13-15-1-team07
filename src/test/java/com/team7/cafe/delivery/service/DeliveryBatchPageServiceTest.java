package com.team7.cafe.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import com.team7.cafe.delivery.dto.DeliveryBatchResponse;
import com.team7.cafe.delivery.dto.PageResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.OrderItemRow;
import com.team7.cafe.global.error.ApiException;

@DisplayName("A14 배송 묶음 페이징 서비스")
class DeliveryBatchPageServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 4);

    private final DeliveryQueryRepository repository = mock(DeliveryQueryRepository.class);
    private final DeliveryService service = new DeliveryService(repository);

    private OrderItemRow row(long orderId, String email, long menuId, int unitPrice, int quantity) {
        return new OrderItemRow(orderId, email, "서울시 실습로 10", "01234", "PLACED", menuId, "메뉴" + menuId,
                unitPrice, quantity, (long) unitPrice * quantity);
    }

    @Test
    @DisplayName("고른 이메일들만 읽어 묶음으로 조립하고 페이지 정보를 채운다")
    void assemblesBatchesOfThePage() {
        // given: 전체 5명 중 size 2의 첫 페이지(a, b)
        Pageable pageable = PageRequest.of(0, 2);
        when(repository.findPlacedEmailsByShippingDate(DATE, pageable))
                .thenReturn(new PageImpl<>(List.of("a@x.com", "b@x.com"), pageable, 5));
        when(repository.findPlacedByShippingDateAndEmails(DATE, List.of("a@x.com", "b@x.com"))).thenReturn(List.of(
                row(1, "a@x.com", 1, 5000, 1),
                row(2, "a@x.com", 1, 5000, 2),
                row(3, "b@x.com", 2, 7000, 1)));

        // when
        PageResponse<DeliveryBatchResponse> result = service.findBatchesPage("2026-11-04", "0", "2");

        // then: 묶음 2개, a는 주문 2건을 합산
        assertThat(result.content()).extracting(DeliveryBatchResponse::email).containsExactly("a@x.com", "b@x.com");
        DeliveryBatchResponse a = result.content().get(0);
        assertThat(a.orderIds()).containsExactly(1L, 2L);
        assertThat(a.totalQuantity()).isEqualTo(3);
        assertThat(a.totalAmount()).isEqualTo(15000);
        assertThat(result.number()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("고객이 없는 페이지는 행을 읽지 않고 빈 목록이다. page·size를 생략하면 0 / 10")
    void emptyPageAndDefaults() {
        // given
        when(repository.findPlacedEmailsByShippingDate(any(LocalDate.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        // when
        PageResponse<DeliveryBatchResponse> result = service.findBatchesPage("2026-11-04", null, null);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(repository).findPlacedEmailsByShippingDate(DATE, PageRequest.of(0, 10));
        verify(repository, never()).findPlacedByShippingDateAndEmails(any(), anyList());
    }

    @Test
    @DisplayName("shippingDate·page·size가 틀리면 400이고 DB를 읽지 않는다")
    void invalidParams() {
        // given: page와 size가 모두 틀림
        // when & then
        assertThatThrownBy(() -> service.findBatchesPage("2026-11-04", "-1", "0"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.fieldErrors()).containsOnlyKeys("page", "size"));
        assertThatThrownBy(() -> service.findBatchesPage("2026-02-30", "0", "10"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.fieldErrors()).containsKey("shippingDate"));
        verifyNoInteractions(repository);
    }
}
