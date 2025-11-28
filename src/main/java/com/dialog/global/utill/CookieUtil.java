package com.dialog.global.utill;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieUtil {

    @Value("${cookie.domain:}")
    private String cookieDomain;
	
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

    // 쿠키를 생성해서 바로 Response에 담는 메서드 (Repository에서 사용)
    public void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // 기존의 createCookie 메서드를 재활용하여 도메인/경로 설정 유지
        Cookie cookie = createCookie(name, value, maxAge, true); 
        response.addCookie(cookie);
    }

    // 쿠키를 삭제하고 Response에 반영하는 메서드 (Repository에서 사용)
    public void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    // 값은 null, 수명은 0으로 설정해서 덮어씌움 (삭제 효과)
                    Cookie cookieToDelete = createCookie(name, null, 0, true);
                    response.addCookie(cookieToDelete);
                }
            }
        }
    }
    // 내부적으로 사용하는 공통 메서드
    private Cookie createCookie(String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(false); // 배포(HTTPS) 환경이면 true로 변경 필요
        cookie.setPath("/");
        // 도메인이 설정되어 있을 때만 적용 (로컬에서는 비워둠)
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    // 객체를 직렬화해서 쿠키 값으로 변환
    public String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    // 쿠키 값을 역직렬화해서 객체로 변환
    public <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}
