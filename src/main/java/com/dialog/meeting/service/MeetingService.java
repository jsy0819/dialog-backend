package com.dialog.meeting.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.dialog.actionitem.domain.ActionItem;
import com.dialog.calendarevent.domain.CalendarEvent;
import com.dialog.calendarevent.domain.EventType;
import com.dialog.calendarevent.repository.CalendarEventRepository;
import com.dialog.keyword.domain.Keyword;
import com.dialog.keyword.domain.KeywordSource;
import com.dialog.keyword.domain.MeetingResultKeyword;
import com.dialog.keyword.repository.KeywordRepository;
import com.dialog.keyword.repository.MeetingResultKeywordRepository;
import com.dialog.meeting.domain.AISummaryResponse;
import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.domain.MeetingCreateRequestDto;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.domain.MeetingFinishRequestDto;
import com.dialog.meeting.domain.MeetingUpdateResultDto;
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.meetingresult.domain.ImportanceLevel;
import com.dialog.meetingresult.domain.MeetingResult;
import com.dialog.meetingresult.repository.MeetingResultRepository;
import com.dialog.participant.domain.Participant;
import com.dialog.participant.repository.ParticipantRepository;
import com.dialog.recording.domain.Recording;
import com.dialog.recording.repository.RecordingRepository;
import com.dialog.transcript.domain.Transcript;
import com.dialog.transcript.repository.TranscriptRepository;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MeetingService {

	private final MeetingRepository meetingRepository;
	private final MeetingResultRepository meetingResultRepository;
	private final MeetingResultKeywordRepository meetingResultKeywordRepository;
	private final MeetUserRepository meetUserRepository;
	private final ParticipantRepository participantRepository;
	private final KeywordRepository keywordRepository;
	private final RecordingRepository recordingRepository;
	private final TranscriptRepository transcriptRepository;
	private final CalendarEventRepository calendarEventRepository;
	
	private final RestTemplate restTemplate;
	@Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

	// 1. 회의 생성
	@Transactional
	public MeetingCreateResponseDto createMeeting(MeetingCreateRequestDto requestDto, Long hostUserId)
			throws IllegalAccessException {

		MeetUser hostUser = meetUserRepository.findById(hostUserId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		LocalDateTime scheduledAt;
		try {
			scheduledAt = LocalDateTime.parse(requestDto.getScheduledAt());
		} catch (DateTimeParseException e) {
			throw new IllegalAccessException("잘못된 날짜 형식입니다. yyyy-MM-dd'T'HH:mm:ss 형식으로 보내야 합니다.");
		}

		// 키워드 리스트를 콤마로 구분된 문자열로 변환하여 저장 (단순 하이라이트용)
		String highlightKeywords = null;
		if (requestDto.getKeywords() != null && !requestDto.getKeywords().isEmpty()) {
			highlightKeywords = String.join(",", requestDto.getKeywords());
		}

		Meeting newMeeting = Meeting.builder()
				.title(requestDto.getTitle())
				.description(requestDto.getDescription())
				.scheduledAt(scheduledAt)
				.hostUser(hostUser)
				.highlightKeywords(highlightKeywords)
				.build();

		Meeting savedMeeting = meetingRepository.save(newMeeting);

		// 참석자 저장
		List<Participant> participantEntities = new ArrayList<>();
		if (requestDto.getParticipants() != null) {
			for (String speakerId : requestDto.getParticipants()) {
				Participant participant = Participant.builder()
						.meeting(savedMeeting)
						.speakerId(speakerId)
						.name(speakerId)
						.build();
				participantEntities.add(participant);
			}
			participantRepository.saveAll(participantEntities);
		}
		
		// 캘린더 이벤트 생성 및 저장
        CalendarEvent calendarEvent = CalendarEvent.builder()
                .userId(hostUser.getId())
                .title(savedMeeting.getTitle())
                .eventDate(savedMeeting.getScheduledAt().toLocalDate())
                .eventTime(savedMeeting.getScheduledAt().toLocalTime())
                .eventType(EventType.MEETING)
                .isImportant(savedMeeting.isImportant()) // Meeting 엔티티에 isImportant() Getter가 있어야 함
                .meeting(savedMeeting)
                .build();

        calendarEventRepository.save(calendarEvent);

		return new MeetingCreateResponseDto(savedMeeting); // 생성자 변경 반영
	}

	// 2. 회의 단건 조회
    public MeetingCreateResponseDto findById(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        // 생성자 하나로 모든 데이터 매핑 처리
        return new MeetingCreateResponseDto(meeting);
    }

    // 3. 전체 회의 조회
    public List<MeetingCreateResponseDto> getAllMeetings() {
        List<Meeting> meetings = meetingRepository.findAll();
        
        // 생성자 사용
        return meetings.stream()
                .map(MeetingCreateResponseDto::new)
                .collect(Collectors.toList());
    }

	// 4. 회의 종료 처리 (상태 변경, 녹음 및 스크립트 저장)
	@Transactional
	public void finishMeeting(Long meetingId, MeetingFinishRequestDto requestDto) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다. ID: " + meetingId));

		meeting.complete();

		// 녹음 파일 정보 저장
		if (requestDto.getRecording() != null) {
			MeetingFinishRequestDto.RecordingData recordingData = requestDto.getRecording();
			if (!recordingRepository.existsByMeetingId(meetingId)) {
				Recording recording = Recording.builder()
						.meeting(meeting)
						.audioFileUrl(recordingData.getAudioFileUrl())
						.audioFileSize(recordingData.getAudioFileSize())
						.audioFormat(recordingData.getAudioFormat())
						.durationSeconds(recordingData.getDurationSeconds())
						.build();
				recordingRepository.save(recording);
			}
		}

		// 스크립트 저장 (기존 데이터 삭제 후 재저장)
		if (requestDto.getTranscripts() != null && !requestDto.getTranscripts().isEmpty()) {
			if (transcriptRepository.existsByMeetingId(meetingId)) {
				transcriptRepository.deleteByMeetingId(meetingId);
			}
			List<Transcript> transcripts = requestDto.getTranscripts().stream()
					.map(t -> Transcript.builder()
							.meeting(meeting)
							.speakerId(t.getSpeakerId())
							.speakerName(t.getSpeakerName())
							.speakerLabel(t.getSpeakerLabel())
							.text(t.getText())
							.startTime(t.getStartTime())
							.endTime(t.getEndTime())
							.sequenceOrder(t.getSequenceOrder())
							.isDeleted(false)
							.build())
					.collect(Collectors.toList());
			transcriptRepository.saveAll(transcripts);
		}
		meetingRepository.save(meeting);
	}

	// 5. 회의 결과(요약, 안건, 키워드, 액션아이템) 저장 및 업데이트
    @Transactional
    public void updateMeetingResult(Long meetingId, MeetingUpdateResultDto updateDto) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        // 기본 정보 업데이트
        meeting.updateInfo(updateDto.getTitle(), null);

        // MeetingResult 조회 또는 생성
        MeetingResult meetingResult = meeting.getMeetingResult();
        if (meetingResult == null) {
            meetingResult = MeetingResult.builder()
                    .meeting(meeting)
                    .build();
        }

        // 중요도 Enum 변환
        ImportanceLevel importance = ImportanceLevel.MEDIUM; // 기본값
        if (updateDto.getImportance() != null && updateDto.getImportance().getLevel() != null) {
            try {
               String levelStr = mapToEnumString(updateDto.getImportance().getLevel());
               importance = ImportanceLevel.valueOf(levelStr);
            } catch (Exception e) { }
        }

        // 요약 정보 업데이트
        meetingResult.updateSummaryInfo(
                updateDto.getPurpose(),
                updateDto.getAgenda(),
                updateDto.getSummary(),
                importance
        );

        // 키워드 업데이트 (Keyword 엔티티 연결)
        if (updateDto.getKeywords() != null) {
            // 1. 기존 연결 관계 모두 삭제 (초기화)
        	meetingResult.getKeywords().clear();
        	meetingResultKeywordRepository.flush();
            
            // 요청된 키워드 리스트 내 중복 방지용 Set
            Set<String> processedKeywords = new HashSet<>();

            for (MeetingUpdateResultDto.KeywordDto kDto : updateDto.getKeywords()) {
                String kName = kDto.getText().trim(); // 공백 제거
                
                // 이미 처리한 단어라면 건너뜀 (List 내 중복 제거)
                if (processedKeywords.contains(kName)) {
                    continue;
                }

                String kSource = kDto.getSource();
                
                // 2. Keyword 엔티티 찾거나 생성
                Keyword keyword = keywordRepository.findByName(kName)
                        .orElseGet(() -> keywordRepository.save(Keyword.builder().name(kName).build()));

                // 3. Source Enum 변환
                KeywordSource sourceEnum;
                if (kSource == null || kSource.isBlank()) {
                    sourceEnum = KeywordSource.USER;
                } else if (kSource.equalsIgnoreCase("USER")) {
                    sourceEnum = KeywordSource.USER;
                } else if (kSource.equalsIgnoreCase("AI")) {
                    sourceEnum = KeywordSource.AI;
                } else {
                    sourceEnum = KeywordSource.USER;
                }

                // 4. 연결 엔티티 생성 및 저장
                MeetingResultKeyword mrk = MeetingResultKeyword.builder()
                        .meetingResult(meetingResult)
                        .keyword(keyword)
                        .source(sourceEnum)
                        .build();
                
                meetingResult.getKeywords().add(mrk);
                
                // 처리된 단어 Set에 추가
                processedKeywords.add(kName);
            }
        }

        // ActionItem 업데이트
        if (updateDto.getActionItems() != null) {
            meetingResult.getActionItems().clear();
            for (MeetingUpdateResultDto.ActionItemDto itemDto : updateDto.getActionItems()) {
                MeetUser assignee = null;
                if (itemDto.getAssignee() != null && !itemDto.getAssignee().isEmpty()) {
                    assignee = meetUserRepository.findByName(itemDto.getAssignee()).orElse(null);
                }
                
                LocalDateTime dueDateTime = null;
                if (itemDto.getDueDate() != null && !itemDto.getDueDate().isEmpty()) {
                    try {
                        LocalDate date = LocalDate.parse(itemDto.getDueDate(), DateTimeFormatter.ISO_DATE);
                        dueDateTime = date.atTime(23, 59, 59);
                    } catch (Exception e) { }
                }

                ActionItem actionItem = ActionItem.builder()
                        .meetingResult(meetingResult)
                        .task(itemDto.getTask())
                        .assignee(assignee)
                        .dueDate(dueDateTime)
                        .isCompleted(false)
                        .source(itemDto.getSource())
                        .build();
                meetingResult.getActionItems().add(actionItem);
            }
        }

        // 발화 로그(Transcript) 업데이트 로직
        if (updateDto.getTranscripts() != null) {
            transcriptRepository.deleteByMeetingId(meeting.getId());
            transcriptRepository.flush(); 

            if (meeting.getTranscripts() != null) {
                meeting.getTranscripts().clear();
            }

            List<Transcript> newTranscripts = new ArrayList<>();
            int order = 0;
            for (MeetingUpdateResultDto.TranscriptDto tDto : updateDto.getTranscripts()) {
                Transcript t = Transcript.builder()
                        .meeting(meeting)
                        .speakerId(tDto.getSpeaker())
                        .speakerName(tDto.getSpeakerName())
                        .text(tDto.getText())
                        .startTime(tDto.getStartTime() != null ? tDto.getStartTime() : 0L)
                        .endTime(tDto.getEndTime() != null ? tDto.getEndTime() : 0L)
                        .sequenceOrder(order++)
                        .isDeleted(false)
                        .build();
                newTranscripts.add(t);
            }
            transcriptRepository.saveAll(newTranscripts);
        }

        meetingResultRepository.save(meetingResult);
        meeting.setMeetingResult(meetingResult);
    }
	
	// AI 요약 생성 요청
	@Transactional
    public MeetingResult generateAISummary(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        List<Transcript> transcripts = transcriptRepository.findByMeetingIdOrderBySequenceOrder(meetingId);
        if (transcripts.isEmpty()) {
            throw new IllegalArgumentException("요약할 대화 내용이 없습니다.");
        }

        // 요청 데이터 구성
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("meeting_id", meetingId);
        
        List<Map<String, Object>> transcriptList = transcripts.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("speaker", t.getSpeakerName() != null ? t.getSpeakerName() : t.getSpeakerId());
            item.put("text", t.getText());
            return item;
        }).collect(Collectors.toList());
        
        requestPayload.put("transcripts", transcriptList);

        String pythonEndpoint = fastApiBaseUrl + "/summary/generate";
        
        try {
            // DTO로 응답 받기
            AISummaryResponse aiResponse = 
                restTemplate.postForObject(pythonEndpoint, requestPayload, AISummaryResponse.class);

            if (aiResponse != null && aiResponse.isSuccess() && aiResponse.getSummary() != null) {
                // DTO 데이터 추출
                AISummaryResponse.AISummaryData data = aiResponse.getSummary();

                MeetingResult meetingResult = meeting.getMeetingResult();
                if (meetingResult == null) {
                    meetingResult = MeetingResult.builder().meeting(meeting).build();
                    meetingResult = meetingResultRepository.save(meetingResult); 
                }

                // 중요도 설정
                ImportanceLevel level = (data.getImportance() != null) ? data.getImportance() : ImportanceLevel.MEDIUM;

                meetingResult.updateSummaryInfo(
                    data.getPurpose(),
                    data.getAgenda(),
                    data.getOverallSummary(),
                    level
                );
                
                // [수정됨] 키워드 저장 로직 (중복 방지 강화)
                List<String> aiKeywords = data.getKeywords();
                if (aiKeywords != null) {
                    // 1. 현재 DB에 저장된 키워드 이름들을 Set으로 추출 (빠른 검색 및 중복 방지)
                    Set<String> existingNames = new HashSet<>();
                    if (meetingResult.getKeywords() != null) {
                        meetingResult.getKeywords().forEach(mrk -> 
                            existingNames.add(mrk.getKeyword().getName().trim())
                        );
                    }

                    for (String kName : aiKeywords) {
                        String normalizedName = kName.trim(); // 공백 제거

                        // 2. 이미 존재하는 키워드라면 저장하지 않고 건너뜀 (DB 에러 방지)
                        if (existingNames.contains(normalizedName)) {
                            continue;
                        }

                        // 3. 키워드 엔티티 조회 또는 생성
                        Keyword keyword = keywordRepository.findByName(normalizedName)
                                .orElseGet(() -> keywordRepository.save(Keyword.builder().name(normalizedName).build()));
                        
                        // 4. 연결 엔티티 생성
                        MeetingResultKeyword mrk = MeetingResultKeyword.builder()
                                .meetingResult(meetingResult)
                                .keyword(keyword)
                                .source(KeywordSource.AI)
                                .build();
                        
                        // 5. 리스트에 추가하고, Set에도 추가하여(AI가 중복 단어를 보낸 경우) 방지
                        meetingResult.getKeywords().add(mrk);
                        existingNames.add(normalizedName);
                    }
                }
                
                return meetingResultRepository.save(meetingResult);

            } else {
                log.error("AI 응답이 비어있거나 실패했습니다: {}", aiResponse);
                throw new RuntimeException("AI 요약 생성 실패: 응답 없음");
            }
        } catch (Exception e) {
            log.error("AI 서버 통신 중 오류 발생: {}", e.getMessage());
            e.printStackTrace(); 
            throw new RuntimeException("AI 요약 생성에 실패했습니다.");
        }
    }
	
	// AI 액션 아이템 생성 요청
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateAllActions(Long meetingId, Map<String, Object> requestData) {
        // Python 서버의 액션 아이템 생성 엔드포인트 (Python 코드에서 확인 필요)
        // 예: main.py에 @app.post("/actions/generate")가 있어야 함
        String pythonEndpoint = fastApiBaseUrl + "/actions/generate"; 
        
        try {
            // 프론트엔드에서 받은 데이터를 그대로 Python으로 전달하거나 필요한 데이터만 추출
            Map<String, Object> aiResponse = restTemplate.postForObject(pythonEndpoint, requestData, Map.class);
            
            if (aiResponse != null) {
                // 응답 구조: { "actions": [ ... ] } 가정
                List<Map<String, Object>> actions = (List<Map<String, Object>>) aiResponse.get("actions");
                
                return Map.of("success", true, "actions", actions != null ? actions : new ArrayList<>());
            } else {
                throw new RuntimeException("AI 서버로부터 빈 응답을 받았습니다.");
            }
        } catch (Exception e) {
            log.error("AI 액션 생성 통신 오류: {}", e.getMessage());
            throw new RuntimeException("액션 아이템 생성 실패: " + e.getMessage());
        }
    }

    private String mapToEnumString(String input) {
        if (input == null) return "MEDIUM";
        switch (input.trim()) {
            case "높음": return "HIGH";
            case "보통": return "MEDIUM";
            case "낮음": return "LOW";
            default: return input.toUpperCase();
        }
    }
}