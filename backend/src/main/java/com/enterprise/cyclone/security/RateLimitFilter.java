package com.enterprise.cyclone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Token-bucket rate limiter for the guarded paths.
 *
 * <p>Buckets are keyed by authenticated principal, falling back to the socket peer address for
 * anonymous traffic. The peer address is read from the connection rather than from
 * {@code X-Forwarded-For}, because a header a client can forge would let an attacker mint unlimited
 * identities; deployments behind a proxy should enable Spring's forward-header handling at the
 * container level instead.
 *
 * <p>A rejected request receives 429 with {@code Retry-After} and a problem document, and is counted
 * before any body is parsed, so a flood cannot consume application work.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int capacity;
    private final Duration refillPeriod;
    private final List<PathPattern> guardedPaths;
    private final ProblemResponseWriter problemWriter;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(SecurityProperties.RateLimit rateLimit, ProblemResponseWriter problemWriter) {
        this.capacity = rateLimit.capacity();
        this.refillPeriod = rateLimit.refillPeriod();
        this.problemWriter = problemWriter;

        PathPatternParser parser = new PathPatternParser();
        this.guardedPaths = rateLimit.paths().stream().map(parser::parse).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isGuarded(request)) {
            chain.doFilter(request, response);
            return;
        }

        TokenBucket bucket = bucketFor(request);
        if (bucket.tryConsume()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, retryAfterSeconds());
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        problemWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded",
                "Request budget of " + capacity + " per " + refillPeriod.toSeconds()
                        + " s exceeded; retry in " + retryAfterSeconds + " s");
    }

    private boolean isGuarded(HttpServletRequest request) {
        PathContainer path = PathContainer.parsePath(request.getRequestURI());
        return guardedPaths.stream().anyMatch(pattern -> pattern.matches(path));
    }

    private TokenBucket bucketFor(HttpServletRequest request) {
        if (buckets.size() > MAX_TRACKED_KEYS) {
            evictIdleBuckets();
        }
        return buckets.computeIfAbsent(keyOf(request), key -> new TokenBucket(capacity, refillPeriod));
    }

    /**
     * Drops buckets that have refilled to capacity: they hold no state worth remembering, and
     * clearing them here is what keeps the map from growing without bound.
     */
    private void evictIdleBuckets() {
        buckets.entrySet().removeIf(entry -> entry.getValue().isIdle());
    }

    private static String keyOf(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "principal:" + authentication.getName();
        }
        return "peer:" + request.getRemoteAddr();
    }

    private long retryAfterSeconds() {
        return (refillPeriod.toMillis() / capacity + 999) / 1000;
    }

    /**
     * A token bucket that refills linearly from empty to {@code capacity} over {@code refillPeriod}.
     */
    private static final class TokenBucket {

        private final int capacity;
        private final double tokensPerNanosecond;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.tokensPerNanosecond = capacity / (double) refillPeriod.toNanos();
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens < 1.0) {
                return false;
            }
            tokens -= 1.0;
            return true;
        }

        synchronized boolean isIdle() {
            refill();
            return tokens >= capacity;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }
            lastRefillNanos = now;
            if (tokens < capacity) {
                tokens = Math.min(capacity, tokens + elapsed * tokensPerNanosecond);
            }
            if (tokens < 0) {
                tokens = 0;
            }
        }
    }
}
