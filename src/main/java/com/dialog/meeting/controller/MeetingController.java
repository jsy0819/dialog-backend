package com.dialog.meeting.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.meeting.domain.MeetingCreateRequestDto;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.domain.MeetingFinishRequestDto;
import com.dialog.meeting.domain.MeetingUpdateResultDto;
import com.dialog.meeting.service.MeetingService;
import com.dialog.meetingresult.domain.MeetingResult;
import com.dialog.security.oauth2.CustomOAuth2User;
import com.dialog.user.service.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    // 1. Create Meeting (POST /api/meetings)
    @PostMapping
    public ResponseEntity<MeetingCreateResponseDto> createMeeting(@RequestBody MeetingCreateRequestDto requestDto,
            Authentication authentication) throws IllegalAccessException {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        Long currentHostUserId;

        if (principal instanceof CustomOAuth2User) {
            currentHostUserId = ((CustomOAuth2User) principal).getMeetuser().getId();
        } else if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            currentHostUserId = userDetails.getId();
        } else if (principal instanceof UserDetails) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MeetingCreateResponseDto responseDto = meetingService.createMeeting(requestDto, currentHostUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // 2. Get My Meetings (GET /api/meetings)
    @GetMapping
    public ResponseEntity<List<MeetingCreateResponseDto>> getMyMeetings(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        Long userId;
        if (principal instanceof CustomOAuth2User) {
            userId = ((CustomOAuth2User) principal).getMeetuser().getId();
        } else if (principal instanceof CustomUserDetails) {
            userId = ((CustomUserDetails) principal).getId();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<MeetingCreateResponseDto> meetings = meetingService.findAllByHostUserId(userId);
        return ResponseEntity.ok(meetings);
    }

    // 3. Get Meeting Detail (GET /api/meetings/{meetingId})
    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingCreateResponseDto> getMeeting(@PathVariable("meetingId") Long meetingId) {
        try {
            MeetingCreateResponseDto responseDto = meetingService.findById(meetingId);
            if (responseDto == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Meeting 조회 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 4. Update Meeting Result (PATCH /api/meetings/{meetingId})
    @PatchMapping("/{meetingId}")
    public ResponseEntity<?> updateMeetingResult(@PathVariable("meetingId") Long meetingId,
            @RequestBody MeetingUpdateResultDto updateDto) {
        try {
            log.info("회의 결과 업데이트 요청 - ID: {}, DTO: {}", meetingId, updateDto);
            meetingService.updateMeetingResult(meetingId, updateDto);
            return ResponseEntity.ok().body("회의 결과가 성공적으로 저장되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("회의 결과 저장 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    // 5. Delete Meeting (DELETE /api/meetings/{meetingId})
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable("meetingId") Long meetingId) {
        try {
            log.info("회의 삭제 요청 - meetingId: {}", meetingId);
            meetingService.deleteMeeting(meetingId); 
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("회의 삭제 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 6. Finish Meeting (POST /api/meetings/{meetingId}/finish)
    @PostMapping("/{meetingId}/finish")
    public ResponseEntity<?> finishMeeting(@PathVariable("meetingId") Long meetingId,
            @RequestBody MeetingFinishRequestDto requestDto) {
        try {
            meetingService.finishMeeting(meetingId, requestDto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("회의를 찾을 수 없습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("회의 종료 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생: " + e.getMessage());
        }
    }

    // 7. Generate AI Summary (POST /api/meetings/summarize)
    @PostMapping("/summarize")
    public ResponseEntity<?> generateSummary(@RequestParam("meetingId") Long meetingId) {
        try {
            MeetingResult result = meetingService.generateAISummary(meetingId);
            
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("purpose", result.getPurpose());
            summaryData.put("agenda", result.getAgenda());
            summaryData.put("overallSummary", result.getSummary());
            summaryData.put("importance", result.getImportance());

            List<String> keywords = result.getKeywords().stream()
                .map(k -> k.getKeyword().getName())
                .collect(Collectors.toList());
            summaryData.put("keywords", keywords);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summaryData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("요약 생성 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류: " + e.getMessage());
        }
    }
    
    // 8. Generate Actions (POST /api/meetings/generate-all-actions)
    @PostMapping("/generate-all-actions")
    public ResponseEntity<?> generateAllActions(
            @RequestParam("meetingId") Long meetingId, 
            @RequestBody Map<String, Object> requestData) {
        try {
            Map<String, Object> result = meetingService.generateAllActions(meetingId, requestData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("액션 아이템 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}