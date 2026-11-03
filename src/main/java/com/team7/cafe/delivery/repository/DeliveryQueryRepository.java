package com.team7.cafe.delivery.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 배송·통계용 조회 전용. 다른 도메인의 엔티티에 의존하지 않도록 orders / order_item 을 직접 읽는다.
 * API를 하나씩 붙이면서 조회 메서드와 행 필드를 늘린다.
 *
 * TODO: 임시 구현. 주문 엔티티(Order/OrderItem)가 dev에 들어오면 JPA 리포지토리(JPQL)로 교체한다.
 *       서비스는 OrderItemRow만 알고 있으므로 이 파일만 바꾸면 된다.
 */
@Repository
public class DeliveryQueryRepository {

    /** 두 조회가 같은 컬럼을 읽으므로 SELECT 목록과 변환을 한 곳에 둔다. */
    private static final String SELECT_ROWS = """
            SELECT o.id, o.email, o.address, o.postcode, o.status,
                   i.menu_id, i.menu_name, i.unit_price, i.quantity, i.line_amount
              FROM orders o
              JOIN order_item i ON i.order_id = o.id
            """;

    private static final RowMapper<OrderItemRow> ROW_MAPPER = (rs, n) -> new OrderItemRow(
            rs.getLong("id"), rs.getString("email"), rs.getString("address"), rs.getString("postcode"),
            rs.getString("status"), rs.getLong("menu_id"), rs.getString("menu_name"),
            rs.getInt("unit_price"), rs.getInt("quantity"), rs.getLong("line_amount"));

    private final JdbcTemplate jdbc;

    public DeliveryQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 주문 한 줄 + 그 주문의 항목 한 줄. */
    public record OrderItemRow(
            long orderId,
            String email,
            String address,
            String postcode,
            String status,
            long menuId,
            String menuName,
            int unitPrice,
            int quantity,
            long lineAmount) {
    }

    /** 접수일(created_at의 서울 날짜)이 같은 모든 주문(취소 포함)의 항목. 하루 범위는 [당일 00:00, 다음 날 00:00). */
    public List<OrderItemRow> findByReceivedDate(LocalDate date) {
        return jdbc.query(SELECT_ROWS + """
                 WHERE o.created_at >= ? AND o.created_at < ?
                 ORDER BY o.id, i.id
                """,
                ROW_MAPPER, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /** 배송일이 같은 PLACED 주문의 항목. 취소된 주문은 배송 대상이 아니므로 제외한다. */
    public List<OrderItemRow> findPlacedByShippingDate(LocalDate shippingDate) {
        return jdbc.query(SELECT_ROWS + """
                 WHERE o.shipping_date = ? AND o.status = 'PLACED'
                 ORDER BY o.id, i.id
                """,
                ROW_MAPPER, shippingDate);
    }
}
