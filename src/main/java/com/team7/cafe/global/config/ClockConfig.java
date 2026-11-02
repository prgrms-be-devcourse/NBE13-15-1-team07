package com.team7.cafe.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 14:00:00 경계 테스트를 위해 시각을 주입 가능하게 둔다. 테스트에서는 Clock.fixed(...)로 교체. */
@Configuration
public class ClockConfig {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SEOUL);
    }
}
