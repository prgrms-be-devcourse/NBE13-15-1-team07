package com.team7.cafe.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

// CacheConfig
// 심화 3 Redis 캐시 설정: 값은 JSON으로, TTL은 30초 (명세 9절: TTL 1초 이상 정수 초) [2026.11.06/김지호]
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .enableSpringCacheNullValueSupport()
                .build();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
