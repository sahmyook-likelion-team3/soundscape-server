package com.soundscape.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/** 메모리 기반 슬라이딩 윈도우 rate limiter. 단일 인스턴스 배포를 전제로 한다. */
@Slf4j
@Component
public class RateLimiter {

    private static final long STALE_ENTRY_MS = Duration.ofMinutes(10).toMillis();

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    public boolean allow(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        Deque<Long> timestamps = requestTimestamps.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /** 10분 이상 요청이 없었던 키는 맵에서 제거한다. 공격자가 매번 다른 IP를 흉내내며 항목만 계속 쌓는 것을 방지. */
    @Scheduled(fixedRate = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void evictStaleKeys() {
        long cutoff = System.currentTimeMillis() - STALE_ENTRY_MS;
        int before = requestTimestamps.size();
        requestTimestamps.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                Long last = timestamps.peekLast();
                return last == null || last < cutoff;
            }
        });
        log.debug("[RateLimiter] 정리 완료: {} -> {}", before, requestTimestamps.size());
    }
}
