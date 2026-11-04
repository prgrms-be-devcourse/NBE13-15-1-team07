package com.team7.cafe.delivery.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    /** 접수일(created_at의 서울 날짜)이 같은 모든 주문(취소 포함)의 항목. 하루 범위는 [당일 00:00, 다음 날 00:00). */
    default List<OrderItemRow> findByReceivedDate(LocalDate date) {
        return findByCreatedAtRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }
}
