package com.team7.cafe.order.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.team7.cafe.global.config.ClockConfig.SEOUL;
import static org.junit.jupiter.api.Assertions.*;

public class ShippingPolicyTest {

    private ShippingPolicy policyAt(LocalDateTime seoulTime) {
        Clock fixed = Clock.fixed(seoulTime.atZone(SEOUL).toInstant(), SEOUL);
        return new ShippingPolicy(fixed);
    }

    private final ShippingPolicy policy = policyAt(LocalDateTime.of(2026, 11, 3, 0, 0));

    // ---배송일 (기본) ---

    @Test
    void 오후_1시59분59초_접수는_당일_배송() {
        LocalDate result = policy.shippingDateOf(LocalDateTime.of(2026, 11, 3, 13, 59, 59));
        assertEquals(LocalDate.of(2026, 11, 3), result);
    }

    @Test
    void 오후_2시_정각_접수는_다음날_배송() {
        LocalDate result = policy.shippingDateOf(LocalDateTime.of(2026, 11, 3, 14, 0, 0));
        assertEquals(LocalDate.of(2026, 11, 4), result);
    }

    //--- 배송일 (심화, 하는김에 포함) ---

    @Test
    void 오후_2시_1초_접수는_다음날_배송() {
        LocalDate result = policy.shippingDateOf(LocalDateTime.of(2026, 11, 3, 14, 0, 1));
        assertEquals(LocalDate.of(2026, 11, 4), result);
    }

    @Test
    void 연말_오후_2시_접수는_다음해_1월1일_배송() {
        LocalDate result = policy.shippingDateOf(LocalDateTime.of(2026, 12, 31, 14, 0, 0));
        assertEquals(LocalDate.of(2027, 1, 1), result);
    }

    //--- 취소 마감 (기본) ---

    @Test
    void 배송일_1시59분59초에는_취소_가능() {
        LocalDate shippingDate = LocalDate.of(2026, 11, 4);
        assertTrue(policy.isCancellable(shippingDate, LocalDateTime.of(2026, 11, 4, 13, 59, 59)));
    }

    @Test
    void 배송일_2시_정각부터는_취소_불가() {
        LocalDate shippingDate = LocalDate.of(2026, 11, 4);
        assertFalse(policy.isCancellable(shippingDate, LocalDateTime.of(2026, 11, 4, 14, 0, 0)));
    }

    @Test
    void 현재시각은_고정한_시계의_서울시각을_돌려준다() {
        ShippingPolicy fixedPolicy = policyAt(LocalDateTime.of(2026, 11, 3, 13, 59, 59));
        assertEquals(LocalDateTime.of(2026, 11, 3, 13, 59, 59), fixedPolicy.now());
    }

}
