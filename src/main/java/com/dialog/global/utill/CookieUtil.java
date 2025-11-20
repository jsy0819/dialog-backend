package com.dialog.global.utill;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CookieUtil {
	
	// 쿠키 조회 메서드
    public Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    // JWT 액세스 토큰 쿠키 생성
    public Cookie createAccessTokenCookie(String token) {
        return createCookie("jwt", token, 60 * 60 * 3, true); // 3시간, HttpOnly=true
    }

    // 리프레시 토큰 쿠키 생성
    public Cookie createRefreshTokenCookie(String token) {
        return createCookie("refreshToken", token, 7 * 24 * 60 * 60, true); // 7일, HttpOnly=true
    }

    // 아이디 기억하기 쿠키 생성 (HttpOnly=false)
    public Cookie createRememberMeCookie(String email) {
        return createCookie("savedEmail", email, 60 * 60 * 24, false); // 1일, HttpOnly=false
    }


    // 쿠키 삭제 (HttpOnly=true 인 보안 쿠키용: AccessToken(jwt), refreshToken)
    public Cookie deleteCookie(String name) {
        return createCookie(name, null, 0, true); 
    }
    
    // 쿠키 삭제 (HttpOnly 설정을 직접 지정: savedEmail 등) 
    public Cookie deleteCookie(String name, boolean httpOnly) {
		return createCookie(name, null, 0, httpOnly);
	}

    // 내부적으로 사용하는 공통 메서드
    private Cookie createCookie(String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(false); // 배포(HTTPS) 환경이면 true로 변경 필요
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }


}
