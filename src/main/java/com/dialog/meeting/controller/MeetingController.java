package com.dialog.meeting.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.domain.MeetingCreateRequestDto;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.service.MeetingService;

import com.dialog.security.jwt.JwtAuthenticationFilter;
import com.dialog.security.oauth2.CustomOAuth2User;
import com.dialog.user.service.CustomUserDetails;
import com.dialog.user.service.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController 
@RequestMapping("/api/meetings")
@RequiredArgsConstructor 
public class MeetingController {

    private final MeetingService meetingService; 

    // 새 회의 생성
    @PostMapping
    public ResponseEntity<MeetingCreateResponseDto> createMeeting(
            @RequestBody MeetingCreateRequestDto requestDto,
            Authentication authentication
    ) throws IllegalAccessException {

        // 인증 정보가 없으면 401 Unauthorized 반환
        if (authentication == null) {
            log.warn("인증 정보 없음 - 인증되지 않은 요청");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal(); // 인증된 사용자 주체 객체
        Long currentHostUserId; // 현재 회의 호스트(생성자) ID

        // 인증 주체(principal) 타입별로 사용자 ID 추출 (소셜/일반 가입 모두 처리)
        if (principal instanceof CustomOAuth2User) {
            currentHostUserId = ((CustomOAuth2User) principal).getMeetuser().getId();
        } else if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            currentHostUserId = userDetails.getId();
        } else if (principal instanceof UserDetails) {
            // UserDetails 기본 구현체(일반 사용)는 CustomUserDetails가 아니라면 거부
            log.warn("인증 객체가 CustomUserDetails가 아님: {}", principal.getClass());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else {
            log.warn("알 수 없는 인증 객체 타입: {}", principal.getClass());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 서비스 계층에 회의 생성 요청 (요청 데이터와 호스트 ID 전달)
        MeetingCreateResponseDto responseDto = meetingService.createMeeting(requestDto, currentHostUserId);

        // 생성된 회의 정보를 담은 DTO와 201(CREATED) 상태로 응답
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
    
	// 특정 회의 상세 조회 엔드포인트
	@GetMapping("/{meetingId}")
	public ResponseEntity<MeetingCreateResponseDto> getMeeting(@PathVariable("meetingId") Long meetingId) {
	    try {
	        // 서비스에서 ID로 회의 엔티티 조회
	    	MeetingCreateResponseDto responseDto = meetingService.findById(meetingId);
	        // 회의를 찾을 수 없는 경우 404 반환
	        if (responseDto == null) {
	            return ResponseEntity.notFound().build();
	        }
	        // Meeting 엔티티를 DTO로 변환해 반환 
	        return ResponseEntity.ok(responseDto);
	    } catch (Exception e) {
	        // 예외 발생 시 500 에러 반환 (서버 로그 기록)
	    	log.error("Meeting 조회 중 예외 발생", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}
}