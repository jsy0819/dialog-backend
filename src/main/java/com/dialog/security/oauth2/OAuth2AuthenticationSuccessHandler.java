package com.dialog.security.oauth2;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.token.domain.RefreshTokenDto;
import com.dialog.token.service.RefreshTokenServiceImpl;
import com.dialog.token.service.UserTokenServiceImpl;
import com.dialog.user.domain.MeetUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler{
   
   private final JwtTokenProvider jwtTokenProvider;
   private final RefreshTokenServiceImpl refreshTokenService;
   private final UserTokenServiceImpl userTokenService;
   private final OAuth2AuthorizedClientService authorizedClientService;
   
    @Value("${app.oauth2.redirect-uri}")
    String redirectUrl ;
    
    @Value("${app.oauth2.fail-uri}")
    String failUrl;
   
   @Override
   public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
           throws IOException, ServletException {
      
       log.info("=== 소셜 로그인 JWT 발급 성공 ===");

       MeetUser user = null;

       try {
           // 1. 사용자 정보 추출
           Object principal = authentication.getPrincipal();
           
           if (principal instanceof CustomOAuth2User) {
               CustomOAuth2User customUser = (CustomOAuth2User) principal;
               user = customUser.getMeetuser();
               log.info("CustomOAuth2User에서 MeetUser 정보를 성공적으로 획득했습니다.");
           } else if (principal instanceof OAuth2User) {               
                log.error("JWT 생성 실패: 인증 주체(Principal)가 CustomOAuth2User 타입이 아닙니다.");
                throw new IllegalStateException("인증 주체에서 MeetUser 객체를 안전하게 가져올 수 없습니다. 재로그인이 필요합니다.");
           } else {
               log.error("JWT 생성 실패: 알 수 없는 Principal 타입입니다.");
               throw new IllegalStateException("알 수 없는 인증 주체 타입입니다. 재로그인이 필요합니다.");
           }
           
           // 2. 액세스 토큰 생성
           String accessToken = jwtTokenProvider.createToken(user);

           // 3. 구글 토큰 추출 및 저장 로직 추가 시작
           if (authentication instanceof OAuth2AuthenticationToken) {
               OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

               OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                   oauthToken.getAuthorizedClientRegistrationId(),
                   oauthToken.getName()
               );

               if (client != null) {
                   String googleAccessToken = client.getAccessToken().getTokenValue();
                   String googleRefreshToken = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;
                   Instant expiresAtInstant = client.getAccessToken().getExpiresAt();
                   LocalDateTime expiresAt = null;
                   if (expiresAtInstant != null) {
                       expiresAt = LocalDateTime.ofInstant(expiresAtInstant, ZoneId.systemDefault());
                   }
                   userTokenService.saveGoogleTokens(user.getId(), googleAccessToken, googleRefreshToken, expiresAt);

                   log.info("구글 액세스 토큰 및 리프레시 토큰 저장 완료");
               } else {
                    log.info("OAuth2AuthorizedClient를 로드하지 못했습니다. 구글 토큰 저장을 건너뜁니다.");
               }
           }

           // 4. 리프레시 토큰 생성 및 DTO 획득
           RefreshTokenDto refreshTokenDto = refreshTokenService.createRefreshTokenDto(user);
           String refreshToken = refreshTokenDto.getRefreshToken();  // 토큰 문자열
           LocalDateTime expiresAt = refreshTokenDto.getExpiresAt(); // 만료 시간
           log.info("리프레시 토큰 만료 시각: {}", expiresAt);

           // 5. 액세스 토큰 쿠키 설정
           Cookie accessTokenCookie = new Cookie("jwt", accessToken);
           accessTokenCookie.setPath("/");
           accessTokenCookie.setHttpOnly(true);  // JS 접근 차단
           accessTokenCookie.setSecure(false);    // HTTPS 환경에서 true 
           accessTokenCookie.setMaxAge(60 * 60 * 24); // 1일           
           response.addCookie(accessTokenCookie);

           // 6. 리프레시 토큰 쿠키 설정
           Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenDto.getRefreshToken());
           refreshTokenCookie.setPath("/");
           refreshTokenCookie.setHttpOnly(true);  // JS 접근 차단
           refreshTokenCookie.setSecure(false);
           // 리프레시 토큰 만료시간까지 쿠키 유효기간 설정 (7일)
           int refreshTokenMaxAge = (int) Duration.between(
                  LocalDateTime.now(), refreshTokenDto.getExpiresAt()).getSeconds();
           refreshTokenCookie.setMaxAge(refreshTokenMaxAge);
           response.addCookie(refreshTokenCookie);

           log.info("액세스 토큰 및 리프레시 토큰 쿠키 설정 완료");

           // 7. 프론트엔드 메인 페이지로 리다이렉트
           getRedirectStrategy().sendRedirect(request, response, redirectUrl);
           log.info("리다이렉트 완료 : {}", redirectUrl);

       } catch (IOException e) {
           log.error("리다이렉트 중 IO 오류 발생: {}", e.getMessage(), e);
           response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Redirect 실패");
       } catch (IllegalStateException e) {
            // MeetUser 획득 실패 시 (CustomOAuth2User 타입이 아니거나 MeetUser를 못 가져온 경우)
            log.error("JWT 발급 실패 (사용자 정보 미획득): {}", e.getMessage());
            response.sendRedirect(failUrl); // 실패 URL로 리다이렉트
       } catch (RuntimeException e) {
           log.error("JWT 생성 중 런타임 오류: {}", e.getMessage(), e);
           response.sendRedirect(failUrl);
       } catch (Exception e) {
           log.error("알 수 없는 오류 발생: {}", e.getMessage(), e);
           response.sendRedirect(failUrl);
       }
   }
}
