package com.team7.cafe.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
 * A24 페이징 쿼리(GROUP BY + LIMIT/OFFSET)를 실제 MySQL로 확인한다.
 * 다른 데이터와 섞이지 않도록 2099년 날짜를 쓰고, 테스트가 끝나면 트랜잭션이 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("A24 고객 통계 페이징 리포지토리 (JPQL)")
class CustomerStatsPagingRepositoryTest {

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

    private void order(String email, LocalDateTime createdAt, boolean cancelled, OrderItem... items) {
        Order order = Order.place(email, "서울시 실습로 10", "01234", createdAt, createdAt.toLocalDate().plusDays(1));
        for (OrderItem item : items) {
            order.addItem(item);
        }
        if (cancelled) {
            order.cancel(createdAt.plusHours(1));
        }
        em.persist(order);
    }

    private OrderItem item(long menuId, int unitPrice, int quantity) {
        return new OrderItem(menuId, menuId == 1 ? "Columbia Nariñó" : "Brazil Serra Do Caparaó", unitPrice, quantity);
    }

    /** 고객 5명: 대소문자만 다른 두 명(A, a), 접수·취소가 섞인 고객, 모두 취소한 고객, 주문 2건인 고객. */
    private void givenFiveCustomers() {
        LocalDateTime t = DAY.atTime(10, 0);
        order("a@x.com", t, false, item(1, 5000, 2), item(2, 7000, 1));      // 소문자 a: PLACED, 수량 3, 17000
        order("A@x.com", t, false, item(1, 5000, 1));                        // 대문자 A: 별개 고객
        order("b@x.com", t, false, item(1, 5000, 1));                        // b: PLACED 1건 + CANCELLED 1건
        order("b@x.com", t.plusMinutes(5), true, item(2, 7000, 4));
        order("c@x.com", t, true, item(1, 5000, 2));                         // c: 모두 취소
        order("d@x.com", t, false, item(1, 5000, 1));                        // d: PLACED 2건
        order("d@x.com", t.plusMinutes(5), false, item(2, 7000, 2));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("고객별로 묶어 이메일 순(대문자 먼저)으로 읽고, 취소 주문은 건수만 세고 수량·금액에서 뺀다")
    void aggregatesPerCustomer() {
        // given
        givenFiveCustomers();

        // when: 한 페이지에 모두 담기는 크기
        Page<CustomerStatRow> page = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(0, 50));

        // then: 대문자 A가 소문자 a보다 먼저, 다른 고객으로 따로 센다
        assertThat(page.getContent()).containsExactly(
                new CustomerStatRow("A@x.com", 1, 1, 1, 5000),
                new CustomerStatRow("a@x.com", 1, 1, 3, 17000),
                new CustomerStatRow("b@x.com", 2, 1, 1, 5000),      // 취소 1건의 수량·금액 4 · 28000은 빠진다
                new CustomerStatRow("c@x.com", 1, 0, 0, 0),         // 모두 취소: 포함되지만 0
                new CustomerStatRow("d@x.com", 2, 2, 3, 19000));
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("size로 자른 한 페이지만 읽고, 마지막은 남은 만큼, 범위를 넘기면 빈 목록이다")
    void readsOnlyRequestedPage() {
        // given: 고객 5명
        givenFiveCustomers();

        // when
        Page<CustomerStatRow> first = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(0, 2));
        Page<CustomerStatRow> second = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(1, 2));
        Page<CustomerStatRow> last = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(2, 2));
        Page<CustomerStatRow> beyond = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(9, 2));

        // then: 2 + 2 + 1, 어느 페이지에서도 전체 고객 수와 페이지 수는 같다
        assertThat(first.getContent()).extracting(CustomerStatRow::email).containsExactly("A@x.com", "a@x.com");
        assertThat(second.getContent()).extracting(CustomerStatRow::email).containsExactly("b@x.com", "c@x.com");
        assertThat(last.getContent()).extracting(CustomerStatRow::email).containsExactly("d@x.com");
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(last.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(beyond.getContent()).isEmpty();       // 범위를 넘긴 페이지는 빈 목록
        assertThat(beyond.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("주문이 없는 날은 빈 목록이고 전체 0, 페이지 0이다")
    void noOrders() {
        // given: 그날 접수된 주문이 없다

        // when
        Page<CustomerStatRow> page = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("접수일 경계: 당일 00:00:00은 포함, 다음 날 00:00:00과 전날 23:59:59는 제외한다")
    void dayBoundary() {
        // given: 하루 경계 바로 안팎의 주문 5건 (end@x.com은 하루 안과 다음 날 00:00:00에 한 건씩, after@x.com은 다음 날 00:00:00)
        order("before@x.com", DAY.atStartOfDay().minusNanos(1_000), false, item(1, 5000, 1));   // 전날 23:59:59.999999
        order("start@x.com", DAY.atStartOfDay(), false, item(1, 5000, 1));                      // 당일 00:00:00
        order("end@x.com", DAY.plusDays(1).atStartOfDay().minusNanos(1_000), false, item(1, 5000, 1));
        order("end@x.com", DAY.plusDays(1).atStartOfDay(), false, item(1, 5000, 1));            // 같은 고객의 다음 날 00:00:00
        order("after@x.com", DAY.plusDays(1).atStartOfDay(), false, item(1, 5000, 1));          // 다른 고객의 다음 날 00:00:00
        em.flush();
        em.clear();

        // when: size를 1로 해서 페이지가 정확히 차게 한다. 덜 찬 첫 페이지는 Spring이 count 쿼리를 생략하고,
        //       전체 수가 내용보다 작으면 보정해 버려서 count 쿼리의 경계가 틀려도 드러나지 않는다.
        Page<CustomerStatRow> first = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(0, 1));
        Page<CustomerStatRow> second = repository.findCustomerStatsByReceivedDate(DAY, PageRequest.of(1, 1));

        // then: 내용과 전체 수(count 쿼리) 모두 경계 안쪽 2명만 센다
        assertThat(first.getContent()).containsExactly(new CustomerStatRow("end@x.com", 1, 1, 1, 5000));
        assertThat(second.getContent()).containsExactly(new CustomerStatRow("start@x.com", 1, 1, 1, 5000));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(second.getTotalElements()).isEqualTo(2);
    }
}
