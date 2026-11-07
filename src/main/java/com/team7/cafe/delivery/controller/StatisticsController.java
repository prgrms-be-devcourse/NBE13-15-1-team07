package com.team7.cafe.delivery.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team7.cafe.delivery.dto.CustomerStatisticsResponse;
import com.team7.cafe.delivery.dto.MenuStatisticsResponse;
import com.team7.cafe.delivery.dto.OrderStatisticsResponse;
import com.team7.cafe.delivery.dto.PageResponse;
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
    public List<MenuStatisticsResponse> menus(@RequestParam String date) {
        return statisticsService.menuStatistics(date);
    }

    /** A18 메뉴 통계 페이징 (심화): 기존 /menus 응답은 그대로 두고 별도 경로로 제공한다. */
    @GetMapping("/menus/page")
    public PageResponse<MenuStatisticsResponse> menusPage(@RequestParam String date,
            @RequestParam(required = false) String page, @RequestParam(required = false) String size) {
        return statisticsService.menuStatisticsPage(date, page, size);
    }

    /** A24 고객 통계 */
    @GetMapping("/customers")
    public List<CustomerStatisticsResponse> customers(@RequestParam String date) {
        return statisticsService.customerStatistics(date);
    }

    /** A24 고객 통계 페이징 (심화): 기존 /customers 응답은 그대로 두고 별도 경로로 제공한다. */
    @GetMapping("/customers/page")
    public PageResponse<CustomerStatisticsResponse> customersPage(@RequestParam String date,
            @RequestParam(required = false) String page, @RequestParam(required = false) String size) {
        return statisticsService.customerStatisticsPage(date, page, size);
    }
}
