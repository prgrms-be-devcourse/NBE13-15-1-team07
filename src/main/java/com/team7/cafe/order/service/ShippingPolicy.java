package com.team7.cafe.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class ShippingPolicy {

    private static final LocalTime CUTOFF = LocalTime.of(14, 0);

    private final Clock clock;

    @Autowired
    public ShippingPolicy(Clock clock) {
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDate shippingDateOf(LocalDateTime orderedAt) {
        if (orderedAt.toLocalTime().isBefore(CUTOFF)) {
            return orderedAt.toLocalDate();
        }
        return orderedAt.toLocalDate().plusDays(1);
    }

    public boolean isCancellable(LocalDate shippingDate, LocalDateTime now) {
        return now.isBefore(shippingDate.atTime(CUTOFF));
    }
}
