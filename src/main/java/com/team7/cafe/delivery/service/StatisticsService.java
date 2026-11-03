package com.team7.cafe.delivery.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
