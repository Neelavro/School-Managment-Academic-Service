package com.example.academic_service.config;

import com.example.academic_service.service.RequestMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Records HTTP request metrics (count, status, duration) into
 * {@link RequestMetricsService}. Runs FIRST in the filter chain so its
 * timing covers everything else. Skips the metrics endpoint itself so
 * the platform admin's polling doesn't skew the numbers.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class RequestMetricsFilter extends OncePerRequestFilter {

    private final RequestMetricsService metrics;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't count our own metric endpoint and the actuator health.
        return path != null && (path.startsWith("/api/platform/metrics")
                || path.startsWith("/actuator/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                     HttpServletResponse res,
                                     FilterChain chain) throws ServletException, IOException {
        long started = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            long duration = System.currentTimeMillis() - started;
            metrics.record(res.getStatus(), duration);
        }
    }
}
