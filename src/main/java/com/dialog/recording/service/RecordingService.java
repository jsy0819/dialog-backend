package com.dialog.recording.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.recording.domain.Recording;
import com.dialog.recording.domain.RecordingCreateRequestDto;
import com.dialog.recording.domain.RecordingResponseDto;
import com.dialog.recording.domain.RecordingUpdateRequestDto;
import com.dialog.recording.repository.RecordingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordingService {

    private final RecordingRepository recordingRepository;
    private final MeetingRepository meetingRepository;

    // 녹음 파일 정보 저장 (Meeting과 연결)
    @Transactional
    public RecordingResponseDto saveRecording(Long meetingId, RecordingCreateRequestDto requestDto) {
        // 1. Meeting 조회
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        // 2. 이미 녹음이 존재하는지 확인
        if (recordingRepository.existsByMeetingId(meetingId)) {
            throw new IllegalStateException("이미 녹음 파일이 존재합니다.");
        }

        // 3. Recording 엔티티 생성
        Recording recording = Recording.builder()
            .meeting(meeting)
            .audioFileUrl(requestDto.getAudioFileUrl())
            .audioFileSize(requestDto.getAudioFileSize())
            .audioFormat(requestDto.getAudioFormat())
            .durationSeconds(requestDto.getDurationSeconds())
            .build();

        // 4. 저장
        Recording savedRecording = recordingRepository.save(recording);

        // 5. Meeting 상태를 COMPLETED로 변경
        meeting.complete();

        return new RecordingResponseDto(savedRecording);
    }

    // Meeting ID로 녹음 조회
    public RecordingResponseDto getRecordingByMeetingId(Long meetingId) {
        Recording recording = recordingRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new IllegalArgumentException("녹음 파일을 찾을 수 없습니다."));

        return new RecordingResponseDto(recording);
    }

    // Recording ID로 녹음 조회
    public RecordingResponseDto getRecordingById(Long recordingId) {
        Recording recording = recordingRepository.findById(recordingId)
            .orElseThrow(() -> new IllegalArgumentException("녹음 파일을 찾을 수 없습니다."));

        return new RecordingResponseDto(recording);
    }

    // 녹음 파일 정보 업데이트
    @Transactional
    public RecordingResponseDto updateRecording(Long recordingId, RecordingUpdateRequestDto requestDto) {
        Recording recording = recordingRepository.findById(recordingId)
            .orElseThrow(() -> new IllegalArgumentException("녹음 파일을 찾을 수 없습니다."));

        recording.completeUpload(
            requestDto.getAudioFileUrl(),
            requestDto.getAudioFileSize(),
            requestDto.getAudioFormat(),
            requestDto.getDurationSeconds()
        );

        return new RecordingResponseDto(recording);
    }

    // 녹음 삭제
    @Transactional
    public void deleteRecording(Long recordingId) {
        Recording recording = recordingRepository.findById(recordingId)
            .orElseThrow(() -> new IllegalArgumentException("녹음 파일을 찾을 수 없습니다."));

        recordingRepository.delete(recording);
    }
}