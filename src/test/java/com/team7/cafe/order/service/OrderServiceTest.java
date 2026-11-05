package com.team7.cafe.order.service;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;
import com.team7.cafe.menu.MenuErrorCode;
import com.team7.cafe.menu.entity.Menu;
import com.team7.cafe.menu.service.MenuService;
import com.team7.cafe.order.OrderErrorCode;
import com.team7.cafe.order.dto.OrderCreateRequest;
import com.team7.cafe.order.dto.OrderItemRequest;
import com.team7.cafe.order.dto.OrderResponse;
import com.team7.cafe.order.entity.Order;
import com.team7.cafe.order.entity.OrderStatus;
import com.team7.cafe.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.team7.cafe.global.config.ClockConfig.SEOUL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 11, 5, 10, 0, 0);
    private static final String EMAIL = "order-test@example.com";
    private static final String ADDRESS = "서울시 실습로 10 101호";
    private static final String POSTCODE = "01234";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuService menuService;

    @Test
    @DisplayName("주문 접수 성공: 메뉴 정보를 복사하고 금액을 계산해 PLACED, 빈 메모로 저장한다")
    void create_success() {
        given(menuService.getEntity(1L)).willReturn(menu(1L, "Columbia Nariñó", 5000));
        given(menuService.getEntity(2L)).willReturn(menu(2L, "Brazil Serra Do Caparaó", 7000));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = serviceAt(BASE_TIME).create(request(List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1))));

        assertEquals(2, response.items().size());
        assertEquals(1L, response.items().get(0).menuId());
        assertEquals("Columbia Nariñó", response.items().get(0).menuName());
        assertEquals(5000, response.items().get(0).unitPrice());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(10000, response.items().get(0).lineAmount());
        assertEquals(17000, response.totalAmount());
        assertEquals(LocalDate.of(2026, 11, 5), response.shippingDate());
        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals("", response.memo());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("같은 메뉴를 두 줄로 담으면 400 VALIDATION_ERROR, fieldErrors에 items")
    void create_duplicateMenu_throws400() {

        OrderCreateRequest request = request(List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(1L, 1)));

        ApiException e = assertThrows(ApiException.class, () -> serviceAt(BASE_TIME).create(request));

        assertEquals(CommonErrorCode.VALIDATION_ERROR, e.errorCode());
        assertTrue(e.fieldErrors().containsKey("items"));
        verifyNoInteractions(menuService, orderRepository);
    }

    @Test
    @DisplayName("판매 중지 메뉴와 없는 메뉴가 함께 있으면 409보다 404 MENU_NOT_FOUND가 먼저")
    void create_unavailableAndMissing_throws404First() {

        Menu soldOut = menu(1L, "Columbia Nariñó", 5000);
        soldOut.changeAvailability(false);
        given(menuService.getEntity(1L)).willReturn(soldOut);
        given(menuService.getEntity(99L)).willThrow(new ApiException(MenuErrorCode.MENU_NOT_FOUND));

        ApiException e = assertThrows(ApiException.class, () -> serviceAt(BASE_TIME).create(request(List.of(
                new OrderItemRequest(1L, 1),
                new OrderItemRequest(99L, 1)))));

        assertEquals(MenuErrorCode.MENU_NOT_FOUND, e.errorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("판매 중지 메뉴를 주문하면 409 MENU_UNAVAILABLE이고 저장하지 않는다")
    void create_unavailableMenu_throws409() {
        Menu soldOut = menu(1L, "Columbia Nariñó", 5000);
        soldOut.changeAvailability(false);
        given(menuService.getEntity(1L)).willReturn(soldOut);

        ApiException e = assertThrows(ApiException.class, () -> serviceAt(BASE_TIME).create(request(List.of(
                new OrderItemRequest(1L, 1)))));

        assertEquals(MenuErrorCode.MENU_UNAVAILABLE, e.errorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("같은 배송 묶음의 기존 주문과 주소가 다르면 409 ADDRESS_MISMATCH이고 저장하지 않는다")
    void create_addressMismatch_throws409() {
        given(menuService.getEntity(1L)).willReturn(menu(1L, "Columbia Nariñó", 5000));
        Order existing = Order.place(EMAIL, "서울시 다른로 20", POSTCODE,
                LocalDateTime.of(2026, 11, 5, 9, 0, 0), LocalDate.of(2026, 11, 5));
        given(orderRepository.findFirstByEmailAndShippingDateAndStatus(EMAIL, LocalDate.of(2026, 11, 5),
                OrderStatus.PLACED)).willReturn(Optional.of(existing));

        ApiException e = assertThrows(ApiException.class, () -> serviceAt(BASE_TIME).create(request(List.of(
                new OrderItemRequest(1L, 1)))));

        assertEquals(OrderErrorCode.ADDRESS_MISMATCH, e.errorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("없는 주문을 상세 조회하면 404 ORDER_NOT_FOUND")
    void getOrder_notFound_throws404() {
        given(orderRepository.findWithItemsById(999L)).willReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> serviceAt(BASE_TIME).getOrder(999L));

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, e.errorCode());
    }

    private OrderService serviceAt(LocalDateTime seoulTime) {
        Clock clock = Clock.fixed(seoulTime.atZone(SEOUL).toInstant(), SEOUL);
        return new OrderService(orderRepository, menuService, new ShippingPolicy(clock));
    }

    private Menu menu(Long id, String name, int price) {
        Menu menu = new Menu(null, name, price, "");
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

    private OrderCreateRequest request(List<OrderItemRequest> items) {
        return new OrderCreateRequest(EMAIL, ADDRESS, POSTCODE, items);
    }
}
