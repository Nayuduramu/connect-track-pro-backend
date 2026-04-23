package com.connecttrack.pro.security;

import com.connecttrack.pro.entity.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000; // 24 hours
    private static final long PASSWORD_CHANGE_TOKEN_VALIDITY = 15 * 60 * 1000; // 15 minutes

    // =========================
    // KEY
    // =========================
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // =========================
    // EXTRACT USERNAME
    // =========================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // =========================
    // EXTRACT CLAIM
    // =========================
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // =========================
    // PARSE TOKEN
    // =========================
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // =========================
    // CHECK EXPIRY
    // =========================
    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // =========================
    // CREATE TOKEN
    // =========================
    private String createToken(Map<String, Object> claims, String subject, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================
    // 🔥 MAIN TOKEN (UPDATED)
    // =========================
    public String generateToken(Employee employee) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("id", employee.getId());
        claims.put("fullName", employee.getFullName());
        claims.put("role", employee.getRole().getName());

        if (employee.getDepartment() != null) {
            claims.put("departmentName", employee.getDepartment().getName());
        }

        if (employee.getJoinDate() != null) {
            claims.put("joinDate", employee.getJoinDate().toString());
        }

        if (employee.getProfilePictureUrl() != null) {
            claims.put("profilePictureUrl", employee.getProfilePictureUrl());
        }

        return createToken(claims, employee.getEmail(), JWT_TOKEN_VALIDITY);
    }

    // =========================
    // PASSWORD CHANGE TOKEN
    // =========================
    public String generatePasswordChangeToken(String email) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("isPasswordChangeToken", true);

        return createToken(claims, email, PASSWORD_CHANGE_TOKEN_VALIDITY);
    }

    // =========================
    // VALIDATE TOKEN
    // =========================
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}