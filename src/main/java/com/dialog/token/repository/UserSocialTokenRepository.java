package com.dialog.token.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.token.domain.UserSocialToken;
import com.dialog.user.domain.MeetUser;

public interface UserSocialTokenRepository extends JpaRepository<UserSocialToken, Long> {
	
    Optional<UserSocialToken> findByUser_IdAndProvider(Long userId, String provider);

    Optional<UserSocialToken> findByRefreshToken(String refreshToken);    
}
