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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.dialog.exception.InvalidJwtTokenException;
import com.dialog.security.oauth2.CustomOAuth2User;
import com.dialog.user.domain.MeetUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {

    // JWT 암호화키 (application.yml에서 주입됨)
    private final SecretKey key;
    private final long validityInMilliseconds;
    private final UserDetailsService userDetailsService;
    
    // 생성자에서 시크릿키, 만료시간 설정해 멤버변수에 저장
    public JwtTokenProvider(@Value("${jwt.secret:DefaultSecretKeyDefaultSecretKeyDefaultSecretKeyDefaultSecretKey}") String secretKey,
                            @Value("${jwt.expiration:3600000}") long validityInMilliseconds,
                            UserDetailsService userDetailsService) {
        // 시크릿키 base64 → 바이트 배열 → HMAC-SHA용 SecretKey 객체 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validityInMilliseconds = validityInMilliseconds;
        this.userDetailsService = userDetailsService;
    }

    // JWT 토큰 발급 메서드: 인증객체 받으면 토큰 생성하여 반환
    public String createToken(Authentication authentication) {
       // 인증객체에 포함된 권한(roles)을 ","로 이어붙여 claim에 저장
        String authorities = authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.validityInMilliseconds);
        
        // principal 객체에서 사용자 이름/이메일을 추출
        Object principal = authentication.getPrincipal();
        String name = "";
        String email = "";

        if (principal instanceof CustomOAuth2User) {
            name = ((CustomOAuth2User) principal).getname();
            email = ((CustomOAuth2User) principal).getEmail();
        } else if (principal instanceof UserDetails) {
            name = ((UserDetails) principal).getUsername();
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            name = (String) principal;
            email = (String) principal;
            // email = ""; // 필요시 처리
        }
        

        return Jwts.builder()
                .subject(email)                 // subject(기본 피식별자)는 email로 지정 (username이 email이면 OK)
                .claim("auth", authorities)    // roles/권한 목록을 claim에 저장
                .claim("name", name)            // 사용자 이름을 claim에 저장
                .claim("email", email)          // 사용자 이메일을 claim에 저장
                .signWith(key)                  // 서명에 SecretKey 사용
                .expiration(validity)           // 만료시간 지정
                .compact();
        
    }
    
    // JWT 토큰 발급 메서드: MeetUser 객체 받으면 토큰 생성 (추가된 로직 - OAuth2 성공 핸들러용)
    public String createToken(MeetUser user) {
    	final String authorities = "ROLE_" + (user.getRole() != null ? user.getRole().name() : "USER"); 

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.validityInMilliseconds);

        return Jwts.builder()
                .subject(user.getEmail())       // subject(기본 피식별자)는 email로 지정
                .claim("auth", authorities)     // roles/권한 목록을 claim에 저장
                .claim("name", user.getName())  // 사용자 이름을 claim에 저장
                .claim("email", user.getEmail())// 사용자 이메일을 claim에 저장
                .signWith(key)
                .expiration(validity)
                .compact();
    }
    // JWT 토큰을 파싱해서 인증객체(Authentication)로 복원하는 메서드
    public Authentication getAuthentication(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Collection<? extends GrantedAuthority> authorities;
            Object authClaim = claims.get("auth");
            if (authClaim != null) {
                authorities = Arrays.stream(authClaim.toString().split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            } else {
                authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
            }

            String username = claims.getSubject();
            UserDetails principal = userDetailsService.loadUserByUsername(username);

            log.info("인증 정보 생성 완료");

            return new UsernamePasswordAuthenticationToken(principal, token, authorities);

        } catch (Exception e) {
            log.error("JWT 인증 정보 추출 중 오류 발생: " + e.getMessage(), e);
            throw new InvalidJwtTokenException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }

    // JWT 유효성 검사 메서드 (서명/만료 등 체크)
    public void validateTokenOrThrow(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.info("토큰이 null이거나 비어있습니다.");
            throw new InvalidJwtTokenException("토큰이 비어있거나 없습니다.");
        }
        try {
            // JWT 파서에 서명키 등록 → 파싱 및 검증
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            log.info("JWT 토큰 검증 성공!");
        } catch (SignatureException e) {
            log.warn("JWT 서명 검증 실패: " + e.getMessage());
            throw new InvalidJwtTokenException("JWT 서명 검증 실패", e);
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 시그니쳐입니다. : " + e.getMessage());
            throw new InvalidJwtTokenException("잘못된 JWT 시그니쳐", e);
        } catch (DecodingException e) {
            log.warn("JWT 디코딩 실패: " + e.getMessage());
            throw new InvalidJwtTokenException("JWT 디코딩 실패", e);
        } catch (ExpiredJwtException e) {
            log.info("만료된 토큰이니 재발급이 필요합니다. : " + e.getMessage());
            throw new InvalidJwtTokenException("만료된 토큰입니다.", e);
        } catch (UnsupportedJwtException e) {
            log.info("지원하지 않는 JWT 토큰입니다. : " + e.getMessage());
            throw new InvalidJwtTokenException("지원하지 않는 JWT 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.info("토큰 형식 틀렸습니다. : " + e.getMessage());
            throw new InvalidJwtTokenException("토큰 형식이 잘못되었습니다.", e);
        } catch (Exception e) {
            log.error("해당 에러를 알수 없습니다. : " + e.getMessage(), e);
            throw new InvalidJwtTokenException("알 수 없는 오류가 발생했습니다.", e);
        }
    }
}