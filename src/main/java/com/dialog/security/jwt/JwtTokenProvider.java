package com.dialog.security.jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {

    // JWT 암호화키 (application.yml에서 주입됨)
    private final SecretKey key;
    private final long validityInMilliseconds;

    // 생성자에서 시크릿키, 만료시간 설정해 멤버변수에 저장
    public JwtTokenProvider(@Value("${jwt.secret:DefaultSecretKeyDefaultSecretKeyDefaultSecretKeyDefaultSecretKey}") String secretKey,
                            @Value("${jwt.expiration:3600000}") long validityInMilliseconds) {
        // 시크릿키 base64 → 바이트 배열 → HMAC-SHA용 SecretKey 객체 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validityInMilliseconds = validityInMilliseconds;
    }

    // JWT 토큰 발급 메서드: 인증객체 받으면 토큰 생성하여 반환
    public String createToken(Authentication authentication) {
        // Spring Security 인증객체에서 모든 권한 추출("ROLE_USER" 등)
        String authorities = authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.validityInMilliseconds);

        // JWT 빌더: subject(이메일), 권한, 만료시간 셋업 → 서명 → String 토큰 반환
        return Jwts.builder()
                .subject(authentication.getName())          // 이메일 등 사용자 PK
                .claim("auth", authorities)                 // 사용자 권한정보
                .signWith(key)                              // 암호화/서명
                .expiration(validity)                       // 만료시간
                .compact();                                 // 최종 JWT 문자열 반환
    }

    // JWT 토큰을 파싱해서 인증객체(Authentication)로 복원하는 메서드
    public Authentication getAuthentication(String token) {
        log.info("JWT에서 인증 정보 추출 시작");
        try {
            // 토큰 검증(서명), Claims에서 주체, 권한 추출
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.info("토큰에서 추출된 subject: " + claims.getSubject());
            log.info("토큰에서 추출된 auth: " + claims.get("auth"));
            Collection<? extends GrantedAuthority> authorities;
            Object authClaim = claims.get("auth");
            if (authClaim != null) {
                // 여러 권한 문자열 → SimpleGrantedAuthority 리스트 변환
                authorities = Arrays.stream(authClaim.toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            } else {
                // 권한 정보 없으면 기본 권한 할당
                authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
            }
            // 인증 정보 객체 생성해서 반환 (principal/토큰/권한)
            UserDetails principal = new User(claims.getSubject(), "", authorities);
            log.info("인증 정보 생성 완료");
            return new UsernamePasswordAuthenticationToken(principal, token, authorities);
        } catch (Exception e) {
            log.info("JWT 인증 정보 추출 중 오류가 발생했습니다. : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // JWT 유효성 검사 메서드 (서명/만료 등 체크)
    public boolean validateToken(String token) {
        log.info("JWT 토큰 검증 시작: " + (token != null ? "토큰 존재" : "토큰 없음"));
        if (token == null || token.trim().isEmpty()) {
            log.info("토큰이 null이거나 비어있습니다. ");
            return false;
        }
        try {
            // JWT 파서에 서명키 등록 → 파싱 및 검증
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            System.out.println("JWT 토큰 검증 성공!");
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT 서명 검증 실패: " + e.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 시그니쳐입니다. : " + e.getMessage());
        } catch (io.jsonwebtoken.io.DecodingException e) {
            log.warn("JWT 디코딩 실패: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("만료된 토큰이니 재발급이 필요합니다. : " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.info("지원하지 않는 JWT 토큰입니다. : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("토큰 형식 틀렸습니다. : " + e.getMessage());
        } catch (Exception e) {
            log.error("해당 에러를 알수 없습니다. : " + e.getMessage(), e);
        }
        return false;
    }
}