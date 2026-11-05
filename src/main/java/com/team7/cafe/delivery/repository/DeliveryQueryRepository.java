package com.team7.cafe.delivery.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.team7.cafe.order.entity.OrderItem;

/**
 * 배송·통계용 조회 전용. 주문 엔티티(Order/OrderItem)에서 필요한 값만 OrderItemRow로 바로 읽는다.
 * 서비스는 OrderItemRow만 알고 있고, 이 인터페이스는 엔티티를 밖으로 내보내지 않는다.
 */
public interface DeliveryQueryRepository extends Repository<OrderItem, Long> {

    /**
     * 접수 시각이 [from, to)인 모든 주문(취소 포함)의 항목.
     * 엔티티의 status는 enum이라 str()로 문자열로 바꿔 OrderItemRow(String)에 맞춘다.
     * 쿼리를 상수로 나누지 않고 통째로 쓴다. 나누면 IDE가 별칭(o, i)을 찾지 못해 경고를 낸다.
     */
    @Query("""
            SELECT new com.team7.cafe.delivery.repository.OrderItemRow(
                   o.id, o.email, o.address, o.postcode, str(o.status),
                   i.menuId, i.menuName, i.unitPrice, i.quantity, i.lineAmount)
              FROM OrderItem i
              JOIN i.order o
             WHERE o.createdAt >= :from AND o.createdAt < :to
             ORDER BY o.id, i.id
            """)
    List<OrderItemRow> findByCreatedAtRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 배송일이 같은 PLACED 주문의 항목. 취소된 주문은 배송 대상이 아니므로 제외한다. */
    @Query("""
            SELECT new com.team7.cafe.delivery.repository.OrderItemRow(
                   o.id, o.email, o.address, o.postcode, str(o.status),
                   i.menuId, i.menuName, i.unitPrice, i.quantity, i.lineAmount)
              FROM OrderItem i
              JOIN i.order o
             WHERE o.shippingDate = :shippingDate AND o.status = com.team7.cafe.order.entity.OrderStatus.PLACED
             ORDER BY o.id, i.id
            """)
    List<OrderItemRow> findPlacedByShippingDate(@Param("shippingDate") LocalDate shippingDate);

    /**
     * A14 페이징 1단계: 배송일이 같은 PLACED 주문이 있는 고객(이메일)을 이메일 순으로 한 페이지만 읽는다.
     * 배송 묶음은 이메일 한 명이 하나이므로 페이지 크기는 묶음 수다. 그룹 쿼리라 count 쿼리를 직접 지정한다.
     */
    @Query(value = """
            SELECT o.email
              FROM Order o
             WHERE o.shippingDate = :shippingDate AND o.status = com.team7.cafe.order.entity.OrderStatus.PLACED
             GROUP BY o.email
             ORDER BY o.email
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o.email)
              FROM Order o
             WHERE o.shippingDate = :shippingDate AND o.status = com.team7.cafe.order.entity.OrderStatus.PLACED
            """)
    Page<String> findPlacedEmailsByShippingDate(@Param("shippingDate") LocalDate shippingDate, Pageable pageable);

    /** A14 페이징 2단계: 1단계로 고른 고객들의 PLACED 주문 항목. 조건은 findPlacedByShippingDate와 같다. */
    @Query("""
            SELECT new com.team7.cafe.delivery.repository.OrderItemRow(
                   o.id, o.email, o.address, o.postcode, str(o.status),
                   i.menuId, i.menuName, i.unitPrice, i.quantity, i.lineAmount)
              FROM OrderItem i
              JOIN i.order o
             WHERE o.shippingDate = :shippingDate AND o.status = com.team7.cafe.order.entity.OrderStatus.PLACED
               AND o.email IN :emails
             ORDER BY o.id, i.id
            """)
    List<OrderItemRow> findPlacedByShippingDateAndEmails(@Param("shippingDate") LocalDate shippingDate,
            @Param("emails") List<String> emails);

    /** 접수일(created_at의 서울 날짜)이 같은 모든 주문(취소 포함)의 항목. 하루 범위는 [당일 00:00, 다음 날 00:00). */
    default List<OrderItemRow> findByReceivedDate(LocalDate date) {
        return findByCreatedAtRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /**
     * A24 페이징 1단계: 고객별 하루 주문 건수(전체·PLACED)를 DB가 GROUP BY로 세고, 요청한 한 페이지(LIMIT/OFFSET)만 읽는다.
     * orders.email이 utf8mb4_bin이라 GROUP BY·ORDER BY가 대소문자를 구분하고 대문자가 먼저 나온다.
     * 그룹 쿼리는 전체 고객 수를 세는 count 쿼리를 직접 지정해야 한다.
     */
    @Query(value = """
            SELECT new com.team7.cafe.delivery.repository.CustomerCountRow(
                   o.email,
                   COUNT(o),
                   SUM(CASE WHEN o.status = com.team7.cafe.order.entity.OrderStatus.PLACED THEN 1 ELSE 0 END))
              FROM Order o
             WHERE o.createdAt >= :from AND o.createdAt < :to
             GROUP BY o.email
             ORDER BY o.email
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o.email)
              FROM Order o
             WHERE o.createdAt >= :from AND o.createdAt < :to
            """)
    Page<CustomerCountRow> findCustomerCounts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            Pageable pageable);

    /** A24 페이징 2단계: 1단계로 고른 고객들의 PLACED 수량·금액 합. 취소 주문은 더하지 않는다. */
    @Query("""
            SELECT new com.team7.cafe.delivery.repository.CustomerSumRow(
                   o.email, SUM(i.quantity), SUM(i.lineAmount))
              FROM OrderItem i
              JOIN i.order o
             WHERE o.status = com.team7.cafe.order.entity.OrderStatus.PLACED
               AND o.createdAt >= :from AND o.createdAt < :to
               AND o.email IN :emails
             GROUP BY o.email
            """)
    List<CustomerSumRow> findPlacedSums(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("emails") List<String> emails);

    /**
     * A18 페이징: PLACED 항목을 (메뉴 ID, 단가, 당시 메뉴명)으로 묶어 DB가 한 페이지만 읽는다.
     * order_item.menu_name은 대소문자를 구분하지 않는 기본 collation이라 BINARY로 묶어야
     * 기존 A18(문자열 그대로 비교)과 같은 결과가 된다. JPQL에는 BINARY가 없어 native 쿼리를 쓴다.
     */
    @Query(value = """
            SELECT i.menu_id AS menuId, MIN(i.menu_name) AS menuName, i.unit_price AS unitPrice,
                   SUM(i.quantity) AS totalQuantity, SUM(i.line_amount) AS totalAmount
              FROM order_item i
              JOIN orders o ON o.id = i.order_id
             WHERE o.status = 'PLACED' AND o.created_at >= :from AND o.created_at < :to
             GROUP BY i.menu_id, i.unit_price, BINARY i.menu_name
             ORDER BY i.menu_id, i.unit_price, BINARY i.menu_name
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT 1
                  FROM order_item i
                  JOIN orders o ON o.id = i.order_id
                 WHERE o.status = 'PLACED' AND o.created_at >= :from AND o.created_at < :to
                 GROUP BY i.menu_id, i.unit_price, BINARY i.menu_name) t
            """,
            nativeQuery = true)
    Page<MenuSumRow> findMenuSums(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    /** 접수일(하루) 기준 메뉴 통계 한 페이지. */
    default Page<MenuSumRow> findMenuSumsByReceivedDate(LocalDate date, Pageable pageable) {
        return findMenuSums(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), pageable);
    }

    /** 접수일(하루) 기준 고객 통계 한 페이지. 두 쿼리의 결과를 이메일로 합친다. 모두 취소한 고객의 수량·금액은 0. */
    default Page<CustomerStatRow> findCustomerStatsByReceivedDate(LocalDate date, Pageable pageable) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        Page<CustomerCountRow> counts = findCustomerCounts(from, to, pageable);
        List<String> emails = counts.getContent().stream().map(CustomerCountRow::email).toList();
        Map<String, CustomerSumRow> sums = new HashMap<>();
        if (!emails.isEmpty()) {
            findPlacedSums(from, to, emails).forEach(sum -> sums.put(sum.email(), sum));
        }

        return counts.map(count -> {
            CustomerSumRow sum = sums.get(count.email());
            return new CustomerStatRow(count.email(), count.orderCount(), count.placedOrderCount(),
                    sum == null ? 0 : sum.totalQuantity(), sum == null ? 0 : sum.totalAmount());
        });
    }
}
