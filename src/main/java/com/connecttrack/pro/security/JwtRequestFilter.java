package com.connecttrack.pro.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // ✅ VERY IMPORTANT: skip public routes
        if (path.startsWith("/api/v1/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/health")
                || path.startsWith("/public/images")
                || path.startsWith("/user-uploads")
                || path.startsWith("/api/v1/user-uploads")) {

            chain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);

            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (ExpiredJwtException e) {
                System.out.println("JWT Token has expired");
            } catch (Exception e) {
                System.out.println("Unable to get JWT Token");
            }
        }

        if (username != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    this.userDetailsService.loadUserByUsername(username);

            // ✅ use existing validateToken(UserDetails)
            if (jwtUtil.validateToken(jwt, userDetails)) {

                UsernamePasswordAuthenticationToken token =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                token.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(token);
            }
        }

        chain.doFilter(request, response);
    }
}