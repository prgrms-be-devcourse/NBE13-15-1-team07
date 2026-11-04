package com.team7.cafe.delivery.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team7.cafe.delivery.dto.CustomerStatisticsResponse;
import com.team7.cafe.delivery.dto.MenuStatisticsResponse;
import com.team7.cafe.delivery.dto.OrderStatisticsResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.OrderItemRow;

/** 통계 날짜는 배송일이 아니라 서울 기준 접수일(created_at의 날짜)이다. 나중에 취소해도 원래 접수일에 반영된다. */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private static final String PLACED = "PLACED";

    private final DeliveryQueryRepository repository;

    public StatisticsService(DeliveryQueryRepository repository) {
        this.repository = repository;
    }

    /** A17. 전체 주문 수는 취소 포함, 수량·금액은 PLACED만. 주문이 없으면 모두 0. */
    public OrderStatisticsResponse orderStatistics(String date) {
        LocalDate parsed = DateParam.parse("date", date);
        Summary s = summarize(repository.findByReceivedDate(parsed));
        return new OrderStatisticsResponse(parsed, s.placedOrders + s.cancelledOrders,
                s.placedOrders, s.cancelledOrders, s.quantity, s.amount);
    }

    /**
     * A18. PLACED만, 메뉴 ID·당시 이름·단가가 같은 항목끼리 합산한다.
     * 취소된 주문에만 있던 메뉴는 PLACED 항목이 없으므로 목록에 나오지 않는다(프론트가 totalQuantity > 0을 요구).
     */
    public List<MenuStatisticsResponse> menuStatistics(String date) {
        LocalDate parsed = DateParam.parse("date", date);

        Map<ItemKey, long[]> sums = new TreeMap<>(ItemKey.ORDER);
        for (OrderItemRow row : repository.findByReceivedDate(parsed)) {
            if (!PLACED.equals(row.status())) {
                continue;
            }
            long[] sum = sums.computeIfAbsent(new ItemKey(row.menuId(), row.menuName(), row.unitPrice()), k -> new long[2]);
            sum[0] += row.quantity();
            sum[1] += row.lineAmount();
        }

        List<MenuStatisticsResponse> result = new ArrayList<>();
        sums.forEach((key, sum) -> result.add(
                new MenuStatisticsResponse(key.menuId(), key.menuName(), key.unitPrice(), sum[0], sum[1])));
        return result;
    }

    /**
     * A24. 이메일 오름차순. 모두 취소한 고객도 포함하며 그 고객의 PLACED 수·수량·금액은 0.
     * 이메일은 가입 형식상 ASCII뿐이라 TreeMap의 문자열 순서가 곧 오름차순이다.
     */
    public List<CustomerStatisticsResponse> customerStatistics(String date) {
        LocalDate parsed = DateParam.parse("date", date);

        // 1단계: 이메일별로 행을 묶는다
        Map<String, List<OrderItemRow>> rowsByEmail = new TreeMap<>();
        for (OrderItemRow row : repository.findByReceivedDate(parsed)) {
            if (!rowsByEmail.containsKey(row.email())) {
                rowsByEmail.put(row.email(), new ArrayList<>());
            }
            rowsByEmail.get(row.email()).add(row);
        }

        // 2단계: 고객마다 A17과 같은 방식으로 집계한다
        List<CustomerStatisticsResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<OrderItemRow>> entry : rowsByEmail.entrySet()) {
            Summary s = summarize(entry.getValue());
            result.add(new CustomerStatisticsResponse(parsed, entry.getKey(),
                    s.placedOrders + s.cancelledOrders, s.placedOrders, s.cancelledOrders, s.quantity, s.amount));
        }
        return result;
    }

    /** 집계 결과. 주문 수는 항목 줄 수가 아니라 주문 ID 기준이고, 수량·금액은 PLACED 항목만 더한다. */
    private record Summary(long placedOrders, long cancelledOrders, long quantity, long amount) {
    }

    /** A17(하루 전체)과 A24(고객 한 명)가 같은 규칙으로 집계하도록 공통으로 쓴다. */
    private static Summary summarize(List<OrderItemRow> rows) {
        Map<Long, String> statusByOrder = new HashMap<>();
        long quantity = 0;
        long amount = 0;
        for (OrderItemRow row : rows) {
            statusByOrder.put(row.orderId(), row.status());
            if (PLACED.equals(row.status())) {
                quantity += row.quantity();
                amount += row.lineAmount();
            }
        }

        long placed = statusByOrder.values().stream().filter(PLACED::equals).count();
        long cancelled = statusByOrder.size() - placed;
        return new Summary(placed, cancelled, quantity, amount);
    }
}
