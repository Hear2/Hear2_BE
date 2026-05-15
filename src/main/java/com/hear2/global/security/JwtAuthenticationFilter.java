package com.hear2.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Long userId = jwtProvider.getUserId(token);
                Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT authentication set. method={}, uri={}, userId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        userId);
            } catch (ResponseStatusException exception) {
                log.debug("JWT authentication failed. method={}, uri={}, reason={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        exception.getReason());
                writeUnauthorized(response, exception.getReason());
                return;
            } catch (RuntimeException exception) {
                log.debug("JWT authentication failed. method={}, uri={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        exception);
                writeUnauthorized(response, "invalid access token");
                return;
            }
        } else {
            log.debug("JWT token missing. method={}, uri={}", request.getMethod(), request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length());
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","message":"%s","timestamp":"%s"}
                """.formatted(escape(message), LocalDateTime.now()));
    }

    private String escape(String value) {
        if (!StringUtils.hasText(value)) {
            return "invalid access token";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
