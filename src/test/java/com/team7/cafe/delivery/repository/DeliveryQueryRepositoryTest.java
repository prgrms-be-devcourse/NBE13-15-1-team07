package com.team7.cafe.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.team7.cafe.order.entity.Order;
import com.team7.cafe.order.entity.OrderItem;

import jakarta.persistence.EntityManager;

/**
 * 서비스 테스트는 리포지토리를 가짜로 쓰므로, JPQL 자체는 여기서 실제 MySQL로 확인한다.
 * 다른 데이터와 섞이지 않도록 2099년 날짜를 쓰고, 테스트가 끝나면 트랜잭션이 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("배송·통계 조회 리포지토리 (JPQL)")
class DeliveryQueryRepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2099, 6, 10);

    @Autowired
    EntityManager em;

    @Autowired
    DeliveryQueryRepository repository;

    @BeforeEach
    void ensureMenus() {
        // order_item.menu_id 외래 키 때문에 메뉴 1·2가 필요하다. 이미 있으면 그대로 두고, 없으면 이 테스트 안에서만 만든다.
        em.createNativeQuery("INSERT IGNORE INTO category (id, name) VALUES (1, '싱글오리진')").executeUpdate();
        em.createNativeQuery("""
                INSERT IGNORE INTO menu (id, category_id, name, price, description, available)
                VALUES (1, 1, 'Columbia Nariñó', 5000, '', TRUE), (2, 1, 'Brazil Serra Do Caparaó', 7000, '', TRUE)
                """).executeUpdate();
    }

    private Order saved(String email, String address, String postcode, LocalDateTime createdAt, LocalDate shippingDate,
            boolean cancelled, OrderItem... items) {
        Order order = Order.place(email, address, postcode, createdAt, shippingDate);
        for (OrderItem item : items) {
            order.addItem(item);
        }
        if (cancelled) {
            order.cancel(createdAt.plusHours(1));
        }
        em.persist(order);
        return order;
    }

    private Order placed(String email, LocalDateTime createdAt, LocalDate shippingDate, OrderItem... items) {
        return saved(email, "서울시 실습로 10", "01234", createdAt, shippingDate, false, items);
    }

    private OrderItem item(long menuId, String name, int unitPrice, int quantity) {
        return new OrderItem(menuId, name, unitPrice, quantity);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("접수일 조회는 [당일 00:00:00, 다음 날 00:00:00) 안의 주문만 읽는다")
    void receivedDateRange() {
        // given: 하루 경계 바로 안팎의 주문 네 건
        LocalDate shipping = DAY.plusDays(1);
        placed("a@x.com", DAY.atStartOfDay().minusNanos(1_000), shipping, item(1, "Columbia Nariñó", 5000, 1));      // 전날 23:59:59.999999
        Order startOfDay = placed("a@x.com", DAY.atStartOfDay(), shipping, item(1, "Columbia Nariñó", 5000, 1));  // 당일 00:00:00
        Order endOfDay = placed("a@x.com", DAY.plusDays(1).atStartOfDay().minusNanos(1_000), shipping,
                item(1, "Columbia Nariñó", 5000, 1));                                                             // 당일 23:59:59.999999
        placed("a@x.com", DAY.plusDays(1).atStartOfDay(), shipping.plusDays(1), item(1, "Columbia Nariñó", 5000, 1)); // 다음 날 00:00:00
        flushAndClear();

        // when
        List<OrderItemRow> rows = repository.findByReceivedDate(DAY);

        // then: 당일 시작과 끝 시각의 주문만 포함된다
        assertThat(rows).extracting(OrderItemRow::orderId).containsExactly(startOfDay.getId(), endOfDay.getId());
    }

    @Test
    @DisplayName("접수일 조회는 취소 주문도 읽고, 상태를 PLACED / CANCELLED 문자열로 돌려준다")
    void receivedDateIncludesCancelled() {
        // given: 접수 주문 한 건과 취소 주문 한 건
        Order placedOrder = placed("a@x.com", DAY.atTime(10, 0), DAY.plusDays(1), item(1, "Columbia Nariñó", 5000, 1));
        Order cancelledOrder = saved("b@x.com", "대전시 중앙로 3", "34000", DAY.atTime(11, 0), DAY.plusDays(1), true,
                item(2, "Brazil Serra Do Caparaó", 7000, 2));
        flushAndClear();

        // when
        List<OrderItemRow> rows = repository.findByReceivedDate(DAY);

        // then
        assertThat(rows).extracting(OrderItemRow::orderId, OrderItemRow::status)
                .containsExactly(
                        tuple(placedOrder.getId(), "PLACED"),
                        tuple(cancelledOrder.getId(), "CANCELLED"));
    }

    @Test
    @DisplayName("주문 한 줄과 항목 한 줄의 값을 그대로 읽고, 주문 ID → 항목 ID 순서로 돌려준다")
    void mapsFieldsAndOrdersRows() {
        // given: 항목이 두 개인 주문 한 건 (항목은 메뉴 2 → 메뉴 1 순서로 저장)
        Order order = saved("a@x.com", "서울시 실습로 10", "01234", DAY.atTime(9, 30), DAY, false,
                item(2, "Brazil Serra Do Caparaó", 7000, 1), item(1, "Columbia Nariñó", 5000, 3));
        flushAndClear();

        // when
        List<OrderItemRow> rows = repository.findByReceivedDate(DAY);

        // then: 저장한 순서(항목 ID 순서)대로, 값은 그대로 매핑된다
        assertThat(rows).containsExactly(
                new OrderItemRow(order.getId(), "a@x.com", "서울시 실습로 10", "01234", "PLACED",
                        2, "Brazil Serra Do Caparaó", 7000, 1, 7000),
                new OrderItemRow(order.getId(), "a@x.com", "서울시 실습로 10", "01234", "PLACED",
                        1, "Columbia Nariñó", 5000, 3, 15000));
    }

    @Test
    @DisplayName("이메일과 주문 당시 메뉴명의 대소문자를 구분해 저장한 그대로 읽는다")
    void keepsCaseOfEmailAndMenuName() {
        // given: 이메일과 메뉴명이 대소문자만 다른 두 주문
        Order upper = placed("A@x.com", DAY.atTime(9, 0), DAY, item(1, "Columbia Nariñó", 5000, 1));
        Order lower = placed("a@x.com", DAY.atTime(10, 0), DAY, item(1, "columbia nariñó", 5000, 1));
        flushAndClear();

        // when
        List<OrderItemRow> rows = repository.findByReceivedDate(DAY);

        // then: 합쳐지거나 소문자로 바뀌지 않는다
        assertThat(rows).extracting(OrderItemRow::orderId, OrderItemRow::email, OrderItemRow::menuName)
                .containsExactly(
                        tuple(upper.getId(), "A@x.com", "Columbia Nariñó"),
                        tuple(lower.getId(), "a@x.com", "columbia nariñó"));
    }

    @Test
    @DisplayName("주문이 없는 날은 빈 목록이다")
    void noOrders() {
        // given: 그날 접수된 주문이 없다

        // when
        List<OrderItemRow> rows = repository.findByReceivedDate(DAY);

        // then
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("배송일 조회는 그 배송일의 PLACED 주문만 읽는다")
    void shippingDateOnlyPlaced() {
        // given: 같은 배송일의 접수·취소 주문, 다른 배송일의 접수 주문
        Order target = placed("a@x.com", DAY.atTime(9, 0), DAY, item(1, "Columbia Nariñó", 5000, 1));
        saved("b@x.com", "대전시 중앙로 3", "34000", DAY.atTime(10, 0), DAY, true, item(2, "Brazil Serra Do Caparaó", 7000, 1));
        placed("c@x.com", DAY.atTime(11, 0), DAY.plusDays(1), item(1, "Columbia Nariñó", 5000, 1));
        flushAndClear();

        // when
        List<OrderItemRow> rows = repository.findPlacedByShippingDate(DAY);

        // then: 취소 주문과 다른 배송일 주문은 빠진다
        assertThat(rows).extracting(OrderItemRow::orderId, OrderItemRow::status)
                .containsExactly(tuple(target.getId(), "PLACED"));
    }

    @Test
    @DisplayName("배송일이 같으면 접수 시각이 달라도 읽고, 배송일에 주문이 없으면 빈 목록이다")
    void shippingDateIgnoresReceivedTime() {
        // given: 배송일이 같고 접수일은 다른 두 주문 (14시 이후 접수는 다음 날 배송)
        Order morning = placed("a@x.com", DAY.atTime(13, 59, 59), DAY, item(1, "Columbia Nariñó", 5000, 1));
        Order previousEvening = placed("a@x.com", DAY.minusDays(1).atTime(14, 0), DAY, item(1, "Columbia Nariñó", 5000, 1));
        flushAndClear();

        // when
        List<OrderItemRow> rows = repository.findPlacedByShippingDate(DAY);
        List<OrderItemRow> empty = repository.findPlacedByShippingDate(DAY.plusDays(5));

        // then
        assertThat(rows).extracting(OrderItemRow::orderId)
                .containsExactlyInAnyOrder(morning.getId(), previousEvening.getId());
        assertThat(empty).isEmpty();
    }
}
