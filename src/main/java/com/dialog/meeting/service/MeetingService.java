package com.dialog.meeting.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.keyword.domain.Keyword;
import com.dialog.keyword.repository.KeywordRepository;
import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.domain.MeetingCreateRequestDto;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.domain.MeetingFinishRequestDto;
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.participant.domain.Participant;
import com.dialog.participant.repository.ParticipantRepository;
import com.dialog.recording.domain.Recording;
import com.dialog.recording.repository.RecordingRepository;
import com.dialog.transcript.domain.Transcript;
import com.dialog.transcript.repository.TranscriptRepository;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

	private final MeetingRepository meetingRepository;
	private final MeetUserRepository meetUserRepository;
	private final ParticipantRepository participantRepository;
	private final KeywordRepository keywordRepository;
	private final RecordingRepository recordingRepository;
	private final TranscriptRepository transcriptRepository;

	// 회의 생성
	@Transactional
	public MeetingCreateResponseDto createMeeting(MeetingCreateRequestDto requestDto, Long hostUserId)
			throws IllegalAccessException {

		// 1. 주최자(User) 엔티티 조회
		MeetUser hostUser = meetUserRepository.findById(hostUserId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		// 2. 빌더 패턴을 사용해 DTO를 Meeting 엔티티로 변환
		LocalDateTime scheduledAt;
		try {
			scheduledAt = LocalDateTime.parse(requestDto.getScheduledAt());
		} catch (DateTimeParseException e) {
			throw new IllegalAccessException("잘못된 날짜 형식입니다. yyyy-MM-dd'T'HH:mm:ss 형식으로 보내야 합니다.");
		}
		Meeting newMeeting = Meeting.builder().title(requestDto.getTitle()).description(requestDto.getDescription())
				.scheduledAt(scheduledAt).hostUser(hostUser).build();

		// 3. 엔티티를 DB에 저장
		Meeting savedMeeting = meetingRepository.save(newMeeting);

		// 4. 참석자 등록
		List<Participant> participantEntities = new ArrayList<>();
		for (String speakerId : requestDto.getParticipants()) {
			Participant participant = Participant.builder().meeting(savedMeeting).speakerId(speakerId).name(speakerId)
					.build();
			participantEntities.add(participant);
		}
		participantRepository.saveAll(participantEntities);

		List<Keyword> keywordEntities = new ArrayList<>();
		if (requestDto.getKeywords() != null) {
			for (String keywordName : requestDto.getKeywords()) {
				// keywordRepository.findByName(keywordName) -> DB 에 해당 키워드가 있는지 먼저 조회
				Keyword keyword = keywordRepository.findByName(keywordName)
						// 없으면 새로 생성해서 DB 에 저장후 반환
						.orElseGet(() -> keywordRepository.save(Keyword.builder().name(keywordName).build()));
				keywordEntities.add(keyword);
			}
			savedMeeting.getKeywords().addAll(keywordEntities);
			meetingRepository.save(savedMeeting);
		}

		// 6. 응답 반환 세팅 (이름/키워드 스트링값만 추출)
		List<String> participantIds = new ArrayList<>();
		for (Participant participant : participantEntities) {
			participantIds.add(participant.getSpeakerId());
		}

		List<String> keywordNames = new ArrayList<>();
		for (Keyword k : keywordEntities) {
			keywordNames.add(k.getName());
		}

		return new MeetingCreateResponseDto(savedMeeting, participantIds, keywordNames);

	}

	public MeetingCreateResponseDto findById(Long meetingId) {
		// 1. 회의 조회
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

		// 2. 해당 회의 id 값을 통해 참가자 조회
		List<Participant> participantEntities = participantRepository.findByMeetingId(meetingId);
		// 3. 참가자의 이름만 뽑아서 List 로 추출
		List<String> participants = new ArrayList<>();
		for (Participant p : participantEntities) {
			participants.add(p.getSpeakerId());
		}

		// 4. 해당 회의 id 값을 통해 하이라이트 조회
		List<Keyword> highlightEntities = keywordRepository.findByMeetingsId(meetingId);
		// 5. 하이라이트의 키워드만 뽑아서 List 로 추출
		List<String> keywords = new ArrayList<>();
		for (Keyword h : highlightEntities) {
			keywords.add(h.getName());
		}
		// 6. List 로 추출한 키워드, 참가자 이름을 DTO로 반환
		return new MeetingCreateResponseDto(meeting, participants, keywords);
	}

	public List<MeetingCreateResponseDto> getAllMeetings() {
		List<Meeting> meetings = meetingRepository.findAll();
		return meetings.stream().map(meeting -> {
			List<String> participantNames = meeting.getParticipants().stream().map(Participant::getName) // 또는
																											// Participant::getSpeakerId
					.collect(Collectors.toList());

			List<String> keywordTexts = meeting.getKeywords().stream().map(Keyword::getName)
					.collect(Collectors.toList());

			return new MeetingCreateResponseDto(meeting, participantNames, keywordTexts);
		}).collect(Collectors.toList());
	}

	// MeetingService.java의 finishMeeting 메서드만 수정
	// 🆕 회의 종료 + Transcript 저장
	@Transactional
	public void finishMeeting(Long meetingId, MeetingFinishRequestDto requestDto) {

		// 1. 회의 조회
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다. ID: " + meetingId));

		// 2. 회의 상태를 COMPLETED로 변경
		meeting.complete();

		// 3. Recording 정보가 있으면 저장
		if (requestDto.getRecording() != null) {
			MeetingFinishRequestDto.RecordingData recordingData = requestDto.getRecording();

			if (!recordingRepository.existsByMeetingId(meetingId)) {
				Recording recording = Recording.builder().meeting(meeting).audioFileUrl(recordingData.getAudioFileUrl())
						.audioFileSize(recordingData.getAudioFileSize()).audioFormat(recordingData.getAudioFormat())
						.durationSeconds(recordingData.getDurationSeconds()).build();

				recordingRepository.save(recording);
			}
		}

		// 🆕 4. Transcript 정보가 있으면 저장 (✅ 활성화됨)
		if (requestDto.getTranscripts() != null && !requestDto.getTranscripts().isEmpty()) {

			// 기존 Transcript가 있다면 삭제 (중복 방지)
			if (transcriptRepository.existsByMeetingId(meetingId)) {
				transcriptRepository.deleteByMeetingId(meetingId);
			}

			// Transcript 엔티티 생성 및 저장
			List<Transcript> transcripts = requestDto.getTranscripts().stream()
					.map(transcriptData -> Transcript.builder().meeting(meeting)
							.speakerId(transcriptData.getSpeakerId()).speakerName(transcriptData.getSpeakerName())
							.speakerLabel(transcriptData.getSpeakerLabel()).text(transcriptData.getText())
							.startTime(transcriptData.getStartTime()).endTime(transcriptData.getEndTime())
							.sequenceOrder(transcriptData.getSequenceOrder()).isDeleted(false) // 기본값 추가
							.build())
					.collect(Collectors.toList());

			transcriptRepository.saveAll(transcripts);
		}

		// 5. 회의 엔티티 저장 (상태 변경 반영)
		meetingRepository.save(meeting);
	}
}
