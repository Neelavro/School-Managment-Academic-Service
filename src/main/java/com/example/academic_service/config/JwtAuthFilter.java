package com.example.academic_service.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Invalid or expired token\",\"data\":null}");
            return;
        }

        Claims claims = jwtUtil.extractClaims(token);
        String phone = claims.getSubject();
        String role = (String) claims.get("role");
        Object userIdRaw = claims.get("userId");
        String userType = (String) claims.get("userType");
        Object staffIdRaw = claims.get("staffId");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                phone, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))
        );
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        if (userIdRaw != null) details.put("userId", userIdRaw);
        if (userType != null) details.put("userType", userType);
        if (staffIdRaw != null) details.put("staffId", staffIdRaw);
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
