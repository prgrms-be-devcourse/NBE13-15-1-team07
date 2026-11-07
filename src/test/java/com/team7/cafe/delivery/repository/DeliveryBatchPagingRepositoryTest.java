package com.team7.cafe.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.team7.cafe.order.entity.Order;
import com.team7.cafe.order.entity.OrderItem;

import jakarta.persistence.EntityManager;

/**
 * A14 페이징 쿼리(이메일 한 페이지 → 그 고객들의 행)를 실제 MySQL로 확인한다.
 * 다른 데이터와 섞이지 않도록 2099년 날짜를 쓰고, 테스트가 끝나면 트랜잭션이 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("A14 배송 묶음 페이징 리포지토리 (JPQL)")
class DeliveryBatchPagingRepositoryTest {

    private static final LocalDate SHIP = LocalDate.of(2099, 6, 13);

    @Autowired
    EntityManager em;

    @Autowired
    DeliveryQueryRepository repository;

    @BeforeEach
    void ensureMenus() {
        em.createNativeQuery("INSERT IGNORE INTO category (id, name) VALUES (1, '싱글오리진')").executeUpdate();
        em.createNativeQuery("""
                INSERT IGNORE INTO menu (id, category_id, name, price, description, available)
                VALUES (1, 1, 'Columbia Nariñó', 5000, '', TRUE), (2, 1, 'Brazil Serra Do Caparaó', 7000, '', TRUE)
                """).executeUpdate();
    }

    private void order(String email, LocalDate shippingDate, boolean cancelled, OrderItem... items) {
        Order order = Order.place(email, "서울시 실습로 10", "01234", shippingDate.minusDays(1).atTime(10, 0), shippingDate);
        for (OrderItem item : items) {
            order.addItem(item);
        }
        if (cancelled) {
            order.cancel(shippingDate.minusDays(1).atTime(11, 0));
        }
        em.persist(order);
    }

    private OrderItem item(long menuId, int quantity) {
        return new OrderItem(menuId, menuId == 1 ? "Columbia Nariñó" : "Brazil Serra Do Caparaó",
                menuId == 1 ? 5000 : 7000, quantity);
    }

    /** A(PLACED 1건), b(PLACED 2건 + 취소 1건), c(취소만), d(PLACED 1건), e(다른 배송일). */
    private void givenOrders() {
        order("A@x.com", SHIP, false, item(1, 1));
        order("b@x.com", SHIP, false, item(1, 1), item(2, 1));
        order("b@x.com", SHIP, false, item(1, 2));
        order("b@x.com", SHIP, true, item(2, 4));
        order("c@x.com", SHIP, true, item(1, 1));
        order("d@x.com", SHIP, false, item(1, 1));
        order("e@x.com", SHIP.plusDays(1), false, item(1, 1));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("PLACED가 있는 고객만 이메일 순(대문자 먼저)으로 size만큼 자르고, 취소만 한 고객과 다른 배송일은 세지 않는다")
    void pagesEmails() {
        // given
        givenOrders();

        // when: size 1이라 페이지가 꽉 차서 count 쿼리도 실제로 실행된다
        Page<String> first = repository.findPlacedEmailsByShippingDate(SHIP, PageRequest.of(0, 1));
        Page<String> second = repository.findPlacedEmailsByShippingDate(SHIP, PageRequest.of(1, 1));
        Page<String> size2 = repository.findPlacedEmailsByShippingDate(SHIP, PageRequest.of(1, 2));
        Page<String> beyond = repository.findPlacedEmailsByShippingDate(SHIP, PageRequest.of(9, 2));

        // then: 고객은 A, b, d 세 명
        assertThat(first.getContent()).containsExactly("A@x.com");
        assertThat(second.getContent()).containsExactly("b@x.com");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(size2.getContent()).containsExactly("d@x.com");
        assertThat(size2.getTotalPages()).isEqualTo(2);
        assertThat(beyond.getContent()).isEmpty();
    }

    @Test
    @DisplayName("고른 고객들의 PLACED 주문 항목만 주문 순으로 읽는다")
    void rowsOfSelectedEmails() {
        // given
        givenOrders();

        // when
        List<OrderItemRow> rows = repository.findPlacedByShippingDateAndEmails(SHIP, List.of("b@x.com", "d@x.com"));

        // then: b의 취소 주문(수량 4)과 다른 고객은 없고, b의 주문 2건 + d의 1건
        assertThat(rows).extracting(OrderItemRow::email).containsExactly("b@x.com", "b@x.com", "b@x.com", "d@x.com");
        assertThat(rows).extracting(OrderItemRow::quantity).containsExactly(1, 1, 2, 1);
        assertThat(rows).extracting(OrderItemRow::status).containsOnly("PLACED");
    }
}
