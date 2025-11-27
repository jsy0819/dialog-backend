package com.dialog.recording.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.recording.domain.RecordingCreateRequestDto;
import com.dialog.recording.domain.RecordingResponseDto;
import com.dialog.recording.domain.RecordingUpdateRequestDto;
import com.dialog.recording.service.RecordingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
public class RecordingController {

	private final RecordingService recordingService;

	// 녹음 파일 정보 저장 (Meeting과 연결) POST /api/recordings?meetingId=1
	@PostMapping
	public ResponseEntity<RecordingResponseDto> createRecording(
			@RequestParam("meetingId") Long meetingId,
			@RequestBody RecordingCreateRequestDto requestDto) {
		try {
			RecordingResponseDto response = recordingService.saveRecording(meetingId, requestDto);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException e) {
			log.error("Recording 생성 실패: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		} catch (IllegalStateException e) {
			log.error("Recording 중복: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

	// Meeting ID로 녹음 조회 GET /api/recordings/meeting/1
	@GetMapping("/meeting/{meetingId}")
	public ResponseEntity<RecordingResponseDto> getRecordingByMeeting(@PathVariable("meetingId") Long meetingId) {
		try {
			RecordingResponseDto response = recordingService.getRecordingByMeetingId(meetingId);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			log.error("Recording 조회 실패: {}", e.getMessage());
			return ResponseEntity.notFound().build();
		}
	}

	// Recording ID로 녹음 조회 GET /api/recordings/1
	@GetMapping("/{recordingId}")
	public ResponseEntity<RecordingResponseDto> getRecording(@PathVariable("recordingId") Long recordingId) {
		try {
			RecordingResponseDto response = recordingService.getRecordingById(recordingId);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			log.error("Recording 조회 실패: {}", e.getMessage());
			return ResponseEntity.notFound().build();
		}
	}

	// 녹음 파일 정보 업데이트 PUT /api/recordings/1
	@PutMapping("/{recordingId}")
	public ResponseEntity<RecordingResponseDto> updateRecording(@PathVariable("recordingId") Long recordingId,
			@RequestBody RecordingUpdateRequestDto requestDto) {
		try {
			RecordingResponseDto response = recordingService.updateRecording(recordingId, requestDto);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			log.error("Recording 업데이트 실패: {}", e.getMessage());
			return ResponseEntity.notFound().build();
		}
	}

	// 녹음 삭제 DELETE /api/recordings/1
	@DeleteMapping("/{recordingId}")
	public ResponseEntity<Void> deleteRecording(@PathVariable("recordingId") Long recordingId) {
		try {
			recordingService.deleteRecording(recordingId);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			log.error("Recording 삭제 실패: {}", e.getMessage());
			return ResponseEntity.notFound().build();
		}
	}
}