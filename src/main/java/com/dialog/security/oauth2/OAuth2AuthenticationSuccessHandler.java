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

import com.dialog.exception.UserNotFoundException;
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
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

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
               // 커스텀 예외 처리, 여기서 발생시키기
               throw new UserNotFoundException("소셜 로그인 사용자 정보를 찾을 수 없습니다. 재로그인 필요");
           } else {
               throw new UserNotFoundException("알 수 없는 Principal 타입입니다. 재로그인 필요");
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
                   //  구글 토큰 로드 실패 시 커스텀 예외 처리 가능
                   log.warn("OAuth2AuthorizedClient를 로드하지 못했습니다. 구글 토큰 저장 건너뜀");
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
           int refreshTokenMaxAge = (int) Duration.between(
                  LocalDateTime.now(), refreshTokenDto.getExpiresAt()).getSeconds();
           refreshTokenCookie.setMaxAge(refreshTokenMaxAge);
           response.addCookie(refreshTokenCookie);

           // 7. 리다이렉트
           getRedirectStrategy().sendRedirect(request, response, redirectUrl);
           log.info("리다이렉트 완료 : {}", redirectUrl);

       } catch (IOException e) {
           log.error("리다이렉트 중 IO 오류 발생: {}", e.getMessage(), e);
           response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Redirect 실패");
       } catch (UserNotFoundException e) {
           // 커스텀 예외 처리, 실패 URL로 리다이렉트 또는 오류 처리
           log.error("사용자 정보 찾기 실패: {}", e.getMessage());
           response.sendRedirect(failUrl);
       } catch (RuntimeException e) {
           log.error("JWT 생성 또는 기타 런타임 오류: {}", e.getMessage(), e);
           response.sendRedirect(failUrl);
       } catch (Exception e) {
           log.error("알 수 없는 오류 발생: {}", e.getMessage(), e);
           response.sendRedirect(failUrl);
       }
   }
}