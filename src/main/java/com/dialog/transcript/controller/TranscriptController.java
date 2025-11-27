package com.dialog.transcript.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dialog.transcript.domain.TranscriptCreateRequestDto;
import com.dialog.transcript.domain.TranscriptResponseDto;
import com.dialog.transcript.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transcripts")
@RequiredArgsConstructor
public class TranscriptController {

    private final TranscriptService transcriptService;

    // 단일 Transcript 저장
    @PostMapping
    public ResponseEntity<TranscriptResponseDto> createTranscript(
            @RequestParam("meetingId") Long meetingId,
            @RequestBody TranscriptCreateRequestDto requestDto) {
        try {
            TranscriptResponseDto response = transcriptService.saveTranscript(meetingId, requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Transcript 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // 일괄 Transcript 저장
    @PostMapping("/batch")
    public ResponseEntity<List<TranscriptResponseDto>> createTranscripts(
            @RequestParam("meetingId") Long meetingId,
            @RequestBody List<TranscriptCreateRequestDto> requestDtos) {
        try {
            List<TranscriptResponseDto> responses = transcriptService.saveTranscripts(meetingId, requestDtos);
            return ResponseEntity.status(HttpStatus.CREATED).body(responses);
        } catch (IllegalArgumentException e) {
            log.error("Transcript 일괄 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // Meeting ID로 Transcript 조회
    @GetMapping("/meeting/{meetingId}")
    public ResponseEntity<List<TranscriptResponseDto>> getTranscriptsByMeeting(
            @PathVariable("meetingId") Long meetingId) {
        try {
            List<TranscriptResponseDto> responses = transcriptService.getTranscriptsByMeetingId(meetingId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Transcript 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 특정 화자의 발화만 조회
    @GetMapping("/meeting/{meetingId}/speaker/{speakerId}")
    public ResponseEntity<List<TranscriptResponseDto>> getTranscriptsBySpeaker(
            @PathVariable("meetingId") Long meetingId,
            @PathVariable("speakerId") String speakerId) {
        try {
            List<TranscriptResponseDto> responses = transcriptService.getTranscriptsBySpeaker(meetingId, speakerId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("화자별 Transcript 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Transcript 삭제
    @DeleteMapping("/{transcriptId}")
    public ResponseEntity<Void> deleteTranscript(@PathVariable("transcriptId") Long transcriptId) {
        try {
            transcriptService.deleteTranscript(transcriptId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Transcript 삭제 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
	// Transcript 복구
    @PatchMapping("/{transcriptId}/restore")
    public ResponseEntity<TranscriptResponseDto> restoreTranscript(@PathVariable("transcriptId") Long transcriptId) {
        try {
            TranscriptResponseDto response = transcriptService.restoreTranscript(transcriptId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Transcript 복구 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Meeting의 모든 Transcript 삭제
    @DeleteMapping("/meeting/{meetingId}")
    public ResponseEntity<Void> deleteTranscriptsByMeeting(@PathVariable("meetingId") Long meetingId) {
        try {
            transcriptService.deleteTranscriptsByMeetingId(meetingId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Meeting Transcript 일괄 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 화자 매핑 업데이트 (특정 화자 ID를 가진 모든 발화의 이름 변경)
    @PatchMapping("/meeting/{meetingId}/speaker")
    public ResponseEntity<Void> updateSpeakerMapping(
            @PathVariable("meetingId") Long meetingId,
            @RequestParam("speakerId") String speakerId,
            @RequestParam("newName") String newName) {
        try {
            transcriptService.updateSpeakerMapping(meetingId, speakerId, newName);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("화자 매핑 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
