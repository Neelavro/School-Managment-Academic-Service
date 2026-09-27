package com.example.academic_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    /** Claim that marks a refresh token; JwtAuthFilter refuses these as access tokens. */
    public static final String TOKEN_TYPE_CLAIM = "tokenType";
    public static final String REFRESH = "refresh";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        return build(subject, extraClaims, expirationMs);
    }

    /** Long-lived token that can only be exchanged for a new access token. */
    public String generateRefreshToken(String subject, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(TOKEN_TYPE_CLAIM, REFRESH);
        return build(subject, claims, refreshExpirationMs);
    }

    private String build(String subject, Map<String, Object> claims, long ttlMs) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            return extractClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
