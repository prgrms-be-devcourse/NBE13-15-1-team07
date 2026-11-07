package com.team7.cafe.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * A18 페이징 쿼리(GROUP BY + LIMIT/OFFSET)를 실제 MySQL로 확인한다.
 * 다른 데이터와 섞이지 않도록 2099년 날짜를 쓰고, 테스트가 끝나면 트랜잭션이 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("A18 메뉴 통계 페이징 리포지토리 (native)")
class MenuStatsPagingRepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2099, 6, 12);

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

    private void order(LocalDateTime createdAt, boolean cancelled, OrderItem... items) {
        Order order = Order.place("m@x.com", "서울시 실습로 10", "01234", createdAt, createdAt.toLocalDate().plusDays(1));
        for (OrderItem item : items) {
            order.addItem(item);
        }
        if (cancelled) {
            order.cancel(createdAt.plusHours(1));
        }
        em.persist(order);
    }

    private OrderItem item(long menuId, String name, int unitPrice, int quantity) {
        return new OrderItem(menuId, name, unitPrice, quantity);
    }

    private List<String> lines(Page<MenuSumRow> page) {
        return page.getContent().stream()
                .map(r -> r.getMenuId() + "/" + r.getMenuName() + "/" + r.getUnitPrice() + "/"
                        + r.getTotalQuantity() + "/" + r.getTotalAmount())
                .toList();
    }

    @Test
    @DisplayName("PLACED만 (메뉴 ID, 단가, 이름)으로 합산하고, 이름의 대소문자가 다르면 따로 센다")
    void aggregates() {
        // given
        LocalDateTime t = DAY.atTime(10, 0);
        order(t, false, item(1, "Columbia", 5000, 2), item(2, "Brazil", 7000, 1));
        order(t, false, item(1, "Columbia", 5000, 1));                   // 같은 묶음에 합산: 수량 3
        order(t, false, item(1, "columbia", 5000, 1));                   // 이름 대소문자만 다름: 별개
        order(t, false, item(1, "Columbia", 6000, 1));                   // 단가 다름: 별개
        order(t, true, item(2, "Brazil", 7000, 4), item(1, "OnlyCancel", 5000, 2));   // 취소: 더하지 않고, 취소에만 있던 메뉴는 없다
        em.flush();
        em.clear();

        // when
        Page<MenuSumRow> page = repository.findMenuSumsByReceivedDate(DAY, PageRequest.of(0, 50));

        // then: 메뉴 ID → 단가 → 이름 순(대문자 먼저)
        assertThat(lines(page)).containsExactly(
                "1/Columbia/5000/3/15000",
                "1/columbia/5000/1/5000",
                "1/Columbia/6000/1/6000",
                "2/Brazil/7000/1/7000");
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("size로 자른 한 페이지만 읽고, 전체 수·페이지 수는 같고, 범위를 넘기면 빈 목록이다")
    void readsOnlyRequestedPage() {
        // given: 묶음 3개
        LocalDateTime t = DAY.atTime(10, 0);
        order(t, false, item(1, "A", 5000, 1), item(2, "C", 7000, 1));
        order(t, false, item(1, "B", 5000, 1));       // 한 주문에는 같은 메뉴를 두 번 넣을 수 없어 주문을 나눈다
        em.flush();
        em.clear();

        // when: size 2 → 2 + 1
        Page<MenuSumRow> first = repository.findMenuSumsByReceivedDate(DAY, PageRequest.of(0, 2));
        Page<MenuSumRow> last = repository.findMenuSumsByReceivedDate(DAY, PageRequest.of(1, 2));
        Page<MenuSumRow> beyond = repository.findMenuSumsByReceivedDate(DAY, PageRequest.of(9, 2));

        // then
        assertThat(lines(first)).containsExactly("1/A/5000/1/5000", "1/B/5000/1/5000");
        assertThat(lines(last)).containsExactly("2/C/7000/1/7000");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(beyond.getContent()).isEmpty();
    }

    @Test
    @DisplayName("접수일 경계: 당일 00:00:00은 포함, 다음 날 00:00:00과 전날 23:59:59는 제외한다")
    void dayBoundary() {
        // given
        order(DAY.atStartOfDay().minusNanos(1_000), false, item(1, "Before", 5000, 1));
        order(DAY.atStartOfDay(), false, item(1, "Start", 5000, 1));
        order(DAY.plusDays(1).atStartOfDay().minusNanos(1_000), false, item(2, "End", 7000, 1));
        order(DAY.plusDays(1).atStartOfDay(), false, item(2, "After", 7000, 1));
        em.flush();
        em.clear();

        // when: size 1로 페이지를 꽉 채워 count 쿼리도 실제로 실행되게 한다
        Page<MenuSumRow> first = repository.findMenuSumsByReceivedDate(DAY, PageRequest.of(0, 1));
        Page<MenuSumRow> second = repository.findMenuSumsByReceivedDate(DAY, PageRequest.of(1, 1));

        // then
        assertThat(lines(first)).containsExactly("1/Start/5000/1/5000");
        assertThat(lines(second)).containsExactly("2/End/7000/1/7000");
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(second.getTotalElements()).isEqualTo(2);
    }
}
