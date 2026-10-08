package com.bradox.erp.sign.service.domain;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Slows down guessing of signing links: after 20 bad tokens from one address in 10 minutes, that address is refused for
 * 15 minutes (SIG-03 security). In memory per instance, which is enough while the app runs as one node.
 */
@Component
class PublicRateLimiter {

    static final int MAX_FAILURES = 20;
    static final Duration WINDOW = Duration.ofMinutes(10);
    static final Duration BLOCK = Duration.ofMinutes(15);

    private record State(Deque<Instant> failures, Instant blockedUntil) {
    }

    private final ConcurrentMap<String, State> byIp = new ConcurrentHashMap<>();

    void check(String ip, Instant now) {
        State s = byIp.get(key(ip));
        if (s != null && s.blockedUntil() != null && s.blockedUntil().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts");
        }
    }

    void failure(String ip, Instant now) {
        byIp.compute(key(ip), (k, old) -> {
            Deque<Instant> q = old == null ? new ArrayDeque<>() : new ArrayDeque<>(old.failures());
            while (!q.isEmpty() && q.peekFirst().isBefore(now.minus(WINDOW))) {
                q.pollFirst();
            }
            q.addLast(now);
            Instant blocked = q.size() >= MAX_FAILURES ? now.plus(BLOCK) : (old == null ? null : old.blockedUntil());
            if (q.size() >= MAX_FAILURES) {
                q.clear();
            }
            return new State(q, blocked);
        });
    }

    private static String key(String ip) {
        return ip == null ? "?" : ip;
    }
}
