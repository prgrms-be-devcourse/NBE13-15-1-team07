package com.team7.cafe.delivery.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team7.cafe.delivery.dto.MenuStatisticsResponse;
import com.team7.cafe.delivery.dto.OrderStatisticsResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository.OrderItemRow;

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
        List<OrderItemRow> rows = repository.findByReceivedDate(parsed);

        // 항목 줄 수가 아니라 주문 ID 기준으로 센다.
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
        return new OrderStatisticsResponse(parsed, placed + cancelled, placed, cancelled, quantity, amount);
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
}
