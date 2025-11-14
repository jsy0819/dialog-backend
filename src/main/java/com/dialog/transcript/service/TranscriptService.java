package com.dialog.transcript.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.transcript.domain.Transcript;
import com.dialog.transcript.domain.TranscriptCreateRequestDto;
import com.dialog.transcript.domain.TranscriptResponseDto;
import com.dialog.transcript.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TranscriptService {

    private final TranscriptRepository transcriptRepository;
    private final MeetingRepository meetingRepository;

    // Transcript 저장 (단일)
    @Transactional
    public TranscriptResponseDto saveTranscript(Long meetingId, TranscriptCreateRequestDto requestDto) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        Transcript transcript = Transcript.builder()
            .meeting(meeting)
            .speakerId(requestDto.getSpeakerId())
            .speakerName(requestDto.getSpeakerName())
            .speakerLabel(requestDto.getSpeakerLabel())
            .text(requestDto.getText())
            .startTime(requestDto.getStartTime())
            .endTime(requestDto.getEndTime())
            .sequenceOrder(requestDto.getSequenceOrder())
            .isDeleted(false)  // 기본값
            .build();

        Transcript savedTranscript = transcriptRepository.save(transcript);
        return new TranscriptResponseDto(savedTranscript);
    }

    // Transcript 일괄 저장
    @Transactional
    public List<TranscriptResponseDto> saveTranscripts(Long meetingId, List<TranscriptCreateRequestDto> requestDtos) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        List<Transcript> transcripts = requestDtos.stream()
            .map(dto -> Transcript.builder()
                .meeting(meeting)
                .speakerId(dto.getSpeakerId())
                .speakerName(dto.getSpeakerName())
                .speakerLabel(dto.getSpeakerLabel())
                .text(dto.getText())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .sequenceOrder(dto.getSequenceOrder())
                .isDeleted(false)  // 기본값
                .build())
            .collect(Collectors.toList());

        List<Transcript> savedTranscripts = transcriptRepository.saveAll(transcripts);
        
        return savedTranscripts.stream()
            .map(TranscriptResponseDto::new)
            .collect(Collectors.toList());
    }

    // Meeting ID로 모든 Transcript 조회
    public List<TranscriptResponseDto> getTranscriptsByMeetingId(Long meetingId) {
        List<Transcript> transcripts = transcriptRepository.findByMeetingIdOrderBySequenceOrder(meetingId);
        
        return transcripts.stream()
            .map(TranscriptResponseDto::new)
            .collect(Collectors.toList());
    }

    // 특정 화자의 발화만 조회
    public List<TranscriptResponseDto> getTranscriptsBySpeaker(Long meetingId, String speakerId) {
        List<Transcript> transcripts = transcriptRepository.findByMeetingIdAndSpeakerId(meetingId, speakerId);
        
        return transcripts.stream()
            .map(TranscriptResponseDto::new)
            .collect(Collectors.toList());
    }

    // Transcript 수정 (텍스트)
    @Transactional
    public TranscriptResponseDto updateTranscriptText(Long transcriptId, String newText) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
            .orElseThrow(() -> new IllegalArgumentException("Transcript를 찾을 수 없습니다."));
        
        transcript.updateText(newText);
        return new TranscriptResponseDto(transcript);
    }

    // Transcript 수정 (화자)
    @Transactional
    public TranscriptResponseDto updateTranscriptSpeaker(Long transcriptId, String newSpeakerId, String newSpeakerName) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
            .orElseThrow(() -> new IllegalArgumentException("Transcript를 찾을 수 없습니다."));
        
        transcript.updateSpeaker(newSpeakerId, newSpeakerName);
        return new TranscriptResponseDto(transcript);
    }

    // Transcript 삭제 (소프트 삭제)
    @Transactional
    public TranscriptResponseDto deleteTranscript(Long transcriptId) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
            .orElseThrow(() -> new IllegalArgumentException("Transcript를 찾을 수 없습니다."));
        
        transcript.delete();
        return new TranscriptResponseDto(transcript);
    }

    // Transcript 복구
    @Transactional
    public TranscriptResponseDto restoreTranscript(Long transcriptId) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
            .orElseThrow(() -> new IllegalArgumentException("Transcript를 찾을 수 없습니다."));
        
        transcript.restore();
        return new TranscriptResponseDto(transcript);
    }

    // Transcript 물리 삭제
    @Transactional
    public void hardDeleteTranscript(Long transcriptId) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
            .orElseThrow(() -> new IllegalArgumentException("Transcript를 찾을 수 없습니다."));
        
        transcriptRepository.delete(transcript);
    }

    // Meeting의 모든 Transcript 삭제
    @Transactional
    public void deleteTranscriptsByMeetingId(Long meetingId) {
        transcriptRepository.deleteByMeetingId(meetingId);
    }
}