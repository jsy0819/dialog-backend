//package com.dialog.security;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import com.dialog.jwt.JwtAuthenticationFilter;
//import com.dialog.jwt.JwtTokenProvider;
//import com.dialog.oauth2.OAuth2AuthenticationFailurHandler;
//import com.dialog.oauth2.OAuth2AuthenticationSuccessHandler;
//import com.dialog.service.CustomOAuth2UserService;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//
//@Slf4j
//@RequiredArgsConstructor
//@Configuration
//public class SecurityConfig {
//
//    private final MeetAuthenticationFaliureHandler faliureHandler;              // 폼로그인 실패 시 처리
//    private final MeetAuthenticationSuccessHandler successHandler;              // 폼로그인 성공 시 처리
//    private final OAuth2AuthenticationFailurHandler oAuth2faliureHandler;        // OAuth2 로그인 실패 시 처리
//    private final OAuth2AuthenticationSuccessHandler oAuth2successHandler;       // OAuth2 로그인 성공 시 처리
//    private final CustomOAuth2UserService customOAuth2UserService;               // OAuth2UserService 커스텀 구현체
//	private final JwtTokenProvider jwtTokenProvider;
//    
//    @Bean
//    public SecurityFilterChain meetFilterChain(HttpSecurity http) throws Exception {
//        return http
//            // 1. CSRF 설정 (h2-console 경로는 CSRF 무시)
//            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"))
////            .httpBasic(httpBasic -> httpBasic.disable())
//            
//            // 2. iframe 정책: 동일 출처만 허용 (h2-console UI 사용 위해)
//            .headers(headers -> headers.frameOptions().sameOrigin())
//            
//            // 3. 세션 관리 ( JWT 토큰 구현 비활성화 )
//            .sessionManagement(session -> session
//                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // 필요 시 세션 생성
//                .maximumSessions(1)                                          // 동시에 세션 1개만 허용
//                .maxSessionsPreventsLogin(true)                             // 중복 로그인 시 이전 세션 만료
//                .expiredUrl("/login?expired=true")								// 세션 만료 시 이동 경로 
//            )
//            
////            .sessionManagement(session -> session
////                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // JWT 사용을 위해 STATELESS로 설정
////                )
//
//            
//            // 4. 권한 설정
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/**", "/login", "/register", "/main", "/", "/static/**", "/css/**", "/js/**", "/images/**", "/h2-console/**").permitAll()  // 누구나 접근 가능
//                //.requestMatchers("/admin").hasRole("ADMIN")  // 어드민 권한 예시
//                .anyRequest().authenticated()  // 그 외 경로는 로그인 인증 필요
//            )
//            
//            // 5. 폼 로그인 설정 ( JWT 토큰 구현 비활성화 )
////            .formLogin(formLogin -> formLogin.disable())
//            .formLogin(login -> login
//                .loginPage("/login")                    // 커스텀 로그인 페이지
//                .usernameParameter("email")         // 로그인 아이디 파라미터명
//                .passwordParameter("password")         // 로그인 비밀번호 파라미터명
//                .defaultSuccessUrl("/")                 // 로그인 성공 시 이동 URL
//                .failureUrl("/login")                   // 로그인 실패 시 이동 URL
//                .successHandler(successHandler)         // 성공 핸들러 (로그 기록 등 추가 처리)
//                .failureHandler(faliureHandler)          // 실패 핸들러 처리
//                .permitAll()                          // 로그인 페이지는 누구나 접근 가능
//            )
//            
//            // 6. OAuth2 로그인 설정
//            .oauth2Login(oauth2 -> oauth2
//                .loginPage("/login")                   // OAuth 로그인 버튼 등 같은 로그인 페이지 사용
//                .userInfoEndpoint(userInfo -> userInfo
//                    .userService(customOAuth2UserService)  // OAuth2UserService 커스텀 구현체 지정
//                )
//                .successHandler(oAuth2successHandler)   // OAuth 로그인 성공 핸들러
//                .failureHandler(oAuth2faliureHandler)    // 실패 핸들러
//            )
//            
//            // 7. 로그아웃 설정 ( JWT 토큰 구현 비활성화 )
////            .logout(logout -> logout.disable())
//            .logout(logout -> logout
//                .logoutUrl("/logout")                  // 로그아웃 처리 경로
//                .logoutSuccessUrl("/")                 // 로그아웃 후 이동 URL
//                .invalidateHttpSession(true)           // 세션 무효화
//                .deleteCookies("JSESSIONID")          // 세션 쿠키 삭제
//                .clearAuthentication(true)            // 인증 정보 초기화
//                .permitAll()                         // 로그아웃 URL 누구나 접근 가능
//            )
//            
//         // JWT 필터 추가.
////        	.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), 
////        			UsernamePasswordAuthenticationFilter.class)
//            
//            // 8. 예외 처리 설정
//            .exceptionHandling(ex -> ex
//                .authenticationEntryPoint((request, response, authException) -> {
//                    log.info("인증이 안된 사용자 입니다.");
//                    response.sendRedirect("/login");  // 비인증시 로그인 페이지로 리다이렉트
//                })
//                .accessDeniedHandler((request, response, accessDeniedException) -> {
//                    log.info("접근 권한이 없습니다.");
//                    response.sendRedirect("/login");  // 권한 없을 경우 로그인 페이지로
//                })
//            )
//            
//            .build();  // FilterChain 빌드 및 반환
//    }
//}