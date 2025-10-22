package com.dialog.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // JWT 생성/검증기 (JwtTokenProvider 의존성 주입)
    private final JwtTokenProvider jwtTokenProvider;

    // 생성자에서 JwtTokenProvider 받아서 저장
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 1. 매 요청(Request)마다 실행되는 필터의 핵심 로직
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 요청 URI 로그(모든 요청의 필터 실행 확인용)
        log.debug("JWT 필터 실행 - URI: {}", request.getRequestURI());

        // 2. 요청에서 JWT 토큰 추출
        String token = resolveToken(request); // 헤더(Authorization) 또는 쿠키에서 토큰 추출
        log.debug("추출된 JWT: {}", token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : "null");

        // 3. 토큰이 존재하고, 유효한 경우
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                // (1) 토큰으로부터 인증객체(Authentication) 생성
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                // (2) SecurityContextHolder에 인증정보 저장 (이후 API에서 인증된 사용자로 동작)
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("JWT 인증 성공 - 사용자: {}", authentication.getName());
            } catch (Exception e) {
                // 인증 중 예외 발생시(토큰 파싱 에러 등)
                log.error("JWT 인증 처리 중 오류", e);
            }
        } else {
            // 토큰이 없거나, 유효하지 않으면 인증없이 다음 필터/요청 진행
            log.debug("JWT 토큰이 없거나 유효하지 않음");
        }

        // 4. 필터 체인에 요청/응답 전달 (다음 필터/서블릿으로 이동)
        chain.doFilter(request, response);
    }

    // 2. JWT 토큰 추출 메서드: Authorization 헤더 또는 쿠키에서 가져옴
    private String resolveToken(HttpServletRequest request) {
        // (1) Authorization 헤더 확인 ("Bearer {jwt}" 형태)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.debug("Authorization 헤더에서 JWT 발견");
            return bearerToken.substring(7); // "Bearer " 제외
        }

        // (2) JWT 쿠키 확인
        Cookie[] cookies = request.getCookies();
        log.debug("쿠키 개수: {}", cookies != null ? cookies.length : 0);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.debug("쿠키: {}={}", cookie.getName(),
                    cookie.getValue() != null
                        ? cookie.getValue().substring(0, Math.min(cookie.getValue().length(), 20)) + "..."
                        : "null");

                // 쿠키 이름이 "jwt"인 경우 JWT 토큰 반환
                if ("jwt".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().trim().isEmpty()) {
                    log.debug("JWT 쿠키에서 토큰 발견");
                    return cookie.getValue();
                }
            }
        }

        // (3) JWT 못찾으면 null 반환
        log.debug("JWT 토큰을 찾을 수 없음");
        return null;
    }
}