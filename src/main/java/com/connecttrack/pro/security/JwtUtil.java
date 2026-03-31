package com.connecttrack.pro.security;

import com.connecttrack.pro.entity.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
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

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(UserDetails userDetails, Employee employee) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", employee.getId());
        claims.put("fullName", employee.getFullName());
        claims.put("role", employee.getRole().getName()); // ✅ Corrected

        if (employee.getDepartment() != null) {
            claims.put("departmentName", employee.getDepartment().getName());
        }
        if (employee.getJoinDate() != null) {
            claims.put("joinDate", employee.getJoinDate().toString());
        }
        if (employee.getProfilePictureUrl() != null) {
            claims.put("profilePictureUrl", employee.getProfilePictureUrl());
        }

        return createToken(claims, userDetails.getUsername());
    }

    public String generatePasswordChangeToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("isPasswordChangeToken", true);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + PASSWORD_CHANGE_TOKEN_VALIDITY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
