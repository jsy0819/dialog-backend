package com.dialog.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.dialog.security.jwt.JwtAuthenticationFilter;
import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.security.oauth2.OAuth2AuthenticationFailurHandler;
import com.dialog.security.oauth2.OAuth2AuthenticationSuccessHandlerJWT;
import com.dialog.user.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@Configuration
public class SecurityConfigJWT {

    private final MeetAuthenticationFaliureHandler faliureHandler;              // 폼 로그인 실패 시 처리기
    private final MeetAuthenticationSuccessHandler successHandler;              // 폼 로그인 성공 시 처리기
    private final OAuth2AuthenticationFailurHandler oAuth2faliureHandler;        // OAuth2 로그인 실패 핸들러
    private final OAuth2AuthenticationSuccessHandlerJWT oAuth2successHandler;		// OAuth2 로그인 성공 핸들러 JWT
//    private final OAuth2AuthenticationSuccessHandler oAuth2successHandler;       // OAuth2 로그인 성공 핸들러
    private final CustomOAuth2UserService customOAuth2UserService;               // OAuth2UserService 커스텀 구현체
    private final JwtTokenProvider jwtTokenProvider;                             // JWT 토큰 생성/검증기

    @Bean
    public SecurityFilterChain meetFilterChain(HttpSecurity http) throws Exception {
        return http
            // 1. CSRF 설정: 특정 경로(h2-console, /api/**)는 CSRF 보호 안함
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"))
            
            // 2. HTTP Basic 인증 비활성화
            .httpBasic(httpBasic -> httpBasic.disable())
            
            // 3. iframe 정책: 동일 출처만 허용 (h2-console 사용 위해)
            .headers(headers -> headers.frameOptions().sameOrigin())
            
            // 4. 세션 설정: JWT 사용 시세션 무상태(stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 5. 권한 설정: 지정된 URL만 무인증 접근 가능, 기타는 인증 필요
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**", "/login", "/register", "/main", "/", "/static/**", "/css/**", "/js/**", "/images/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated()  // 나머지 요청은 인증 필요
            )
            
            // 6. 폼 로그인 비활성화 (JWT 비사용 시 활성화 가능하여 주석 처리)
            .formLogin(formLogin -> formLogin.disable())
            
            // 7. OAuth2 로그인 설정: 커스텀 서비스 및 성공/실패 핸들러 설정
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")                   // OAuth2 로그인 페이지 URL
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)  // OAuth2 사용자 정보 서비스
                )
                .successHandler(oAuth2successHandler)   // 성공 시 처리 핸들러
                .failureHandler(oAuth2faliureHandler)    // 실패 시 처리 핸들러
            )
            
            // 8. 로그아웃 비활성화 (필요시 활성화 가능)
            .logout(logout -> logout.disable())
            
            // 9. JWT 필터 등록: 폼 로그인 전에 실행되도록 함
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
            
            // 10. 인증/권한 관련 예외 처리 설정
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    log.info("인증 안된 사용자 요청");
                    response.sendRedirect("/login");  // 인증 안된 경우 로그인 페이지로 리다이렉트
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.info("접근 권한 없음");
                    response.sendRedirect("/login");  // 권한 없는 경우 로그인 페이지로 리다이렉트
                })
            )
            
            .build();  // SecurityFilterChain 객체 생성 및 반환
    }
}