package com.team7.cafe.delivery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team7.cafe.delivery.dto.OrderStatisticsResponse;
import com.team7.cafe.delivery.service.StatisticsService;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /** A17 주문 통계 */
    @GetMapping("/orders")
    public OrderStatisticsResponse orders(@RequestParam String date) {
        return statisticsService.orderStatistics(date);
    }

    /** A18 메뉴 통계 */
    @GetMapping("/menus")
    public Object menus(@RequestParam String date) {
        throw new UnsupportedOperationException("A18 미구현");
    }

    /** A24 고객 통계 */
    @GetMapping("/customers")
    public Object customers(@RequestParam String date) {
        throw new UnsupportedOperationException("A24 미구현");
    }
}
