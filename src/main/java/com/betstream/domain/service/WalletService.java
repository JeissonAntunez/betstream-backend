package com.betstream.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * WalletService - manages user balances.
 * Uses Redis for fast balance reads with PostgreSQL as source of truth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private String balanceKey(UUID userId) {
        return "wallet:balance:" + userId;
    }

    public Mono<BigDecimal> getBalance(UUID userId) {
        return redisTemplate.opsForValue().get(balanceKey(userId))
                .map(BigDecimal::new)
                .defaultIfEmpty(BigDecimal.ZERO);
    }

    /**
     * Atomically deduct balance. Fails if insufficient funds.
     */
    public Mono<BigDecimal> deductBalance(UUID userId, BigDecimal amount) {
        String key = balanceKey(userId);
        return redisTemplate.opsForValue().get(key)
                .defaultIfEmpty("0")
                .flatMap(balStr -> {
                    BigDecimal current = new BigDecimal(balStr);
                    if (current.compareTo(amount) < 0) {
                        return Mono.error(new IllegalStateException(
                                "Insufficient balance. Current: " + current + ", Required: " + amount));
                    }
                    BigDecimal newBalance = current.subtract(amount);
                    return redisTemplate.opsForValue()
                            .set(key, newBalance.toPlainString())
                            .thenReturn(newBalance);
                });
    }

    public Mono<BigDecimal> creditBalance(UUID userId, BigDecimal amount) {
        String key = balanceKey(userId);
        return redisTemplate.opsForValue().get(key)
                .defaultIfEmpty("0")
                .flatMap(balStr -> {
                    BigDecimal newBalance = new BigDecimal(balStr).add(amount);
                    return redisTemplate.opsForValue()
                            .set(key, newBalance.toPlainString())
                            .thenReturn(newBalance);
                });
    }

    public Mono<Void> initBalance(UUID userId, BigDecimal initialBalance) {
        return redisTemplate.opsForValue()
                .set(balanceKey(userId), initialBalance.toPlainString())
                .then();
    }
}
