package com.dialog.meeting.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 👈 [수정] DTO 패키지 경로로 변경
import com.dialog.meeting.domain.MeetingCreateRequestDto;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.service.MeetingService;

import lombok.RequiredArgsConstructor;
// import jakarta.validation.Valid; 

/**
 * /api/meetings 로 들어오는 회의 관련 API 요청을 처리하는 컨트롤러
 */
@RestController // 이 클래스가 REST API 컨트롤러임을 선언
@RequestMapping("/api/meetings") // 공통 URL 경로
@RequiredArgsConstructor // final 필드(MeetingService) 생성자 주입
public class MeetingController {

    private final MeetingService meetingService; // 서비스 계층 의존성 주입

    /**
     * 새 회의 생성 API (POST /api/meetings)
     */
    @PostMapping
    public ResponseEntity<MeetingCreateResponseDto> createMeeting(
            @RequestBody MeetingCreateRequestDto requestDto // JSON 요청 본문을 DTO로 변환
    ) {
        
        // 1. (인증) 현재 로그인한 사용자 ID(hostUserId)를 가져옵니다.
        //    (TODO: 실제로는 Spring Security 등에서 인증 정보를 가져와야 함)
        Long currentHostUserId = 1L; // (임시 하드코딩)

        // 2. (위임) 서비스 레이어에 DTO와 사용자 ID를 넘겨 로직 처리를 위임
        MeetingCreateResponseDto responseDto = meetingService.createMeeting(requestDto, currentHostUserId);

        // 3. (응답) 처리된 결과(Response DTO)를 201 Created 상태와 함께 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
    
    // TODO: 회의 상세 조회 API (GET /api/meetings/{id}) 등...
}