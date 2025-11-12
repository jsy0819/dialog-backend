package com.dialog.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dialog.exception.InvalidJwtTokenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 1. 매 요청마다 실행되는 필터의 핵심 로직
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 요청 URI 로그
        log.debug("JWT 필터 실행 - URI: {}", request.getRequestURI());

        String uri = request.getRequestURI();
        // 재발급 엔드포인트에서는 토큰 없이도 통과되게 처리!
        if ("/api/reissue".equals(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 요청에서 JWT 토큰 추출
        String token = resolveToken(request); // 헤더 또는 쿠키에서 토큰 추출
        log.debug("추출된 JWT 토큰: {}", token);

        // 3. 토큰이 존재하고, 유효한 경우
        if (token != null) {
            try {
                // 예외 발생 시 catch로 넘어감, 성공하면 인증객체 생성
                jwtTokenProvider.validateTokenOrThrow(token);

                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidJwtTokenException e) {
                log.error("JWT 인증 처리 중 오류 발생:", e);
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return; // 요청 차단
            } catch (Exception e) {
                log.error("JWT 처리 중 알 수 없는 오류 발생:", e);
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            log.debug("JWT 토큰이 없거나 유효하지 않음");
        }

        // 4. 필터 체인에 요청/응답 전달 (다음 필터/서블릿으로 이동)
        chain.doFilter(request, response);
    }

    // 2. JWT 토큰 추출 메서드: Authorization 헤더 또는 쿠키에서 가져옴
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        log.debug("쿠키 개수: {}", cookies != null ? cookies.length : 0);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().trim().isEmpty()) {
                    return cookie.getValue();
                }
            }
        }

        log.debug("JWT 토큰을 찾을 수 없음");
        return null;
    }
}