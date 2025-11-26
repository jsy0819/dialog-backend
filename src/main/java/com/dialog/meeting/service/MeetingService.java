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
import com.dialog.actionitem.repository.ActionItemRepository;
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
    private final ActionItemRepository actionItemRepository;

    private final RestTemplate restTemplate;
    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    // ==================================================================
    // 1. Create (생성)
    // ==================================================================
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

        String highlightKeywords = null;
        if (requestDto.getKeywords() != null && !requestDto.getKeywords().isEmpty()) {
            highlightKeywords = String.join(",", requestDto.getKeywords());
        }

        Meeting newMeeting = Meeting.builder().title(requestDto.getTitle()).description(requestDto.getDescription())
                .scheduledAt(scheduledAt).hostUser(hostUser).highlightKeywords(highlightKeywords).build();

        Meeting savedMeeting = meetingRepository.save(newMeeting);

        // 참석자 저장
        List<Participant> participantEntities = new ArrayList<>();
        if (requestDto.getParticipants() != null) {
            int index = 1;
            for (String name : requestDto.getParticipants()) {
                Participant participant = Participant.builder()
                        .meeting(savedMeeting)
                        .speakerId("Speaker " + index)
                        .name(name)
                        .build();
                participantEntities.add(participant);
                index++;
            }
            participantRepository.saveAll(participantEntities);
        }

        // 캘린더 이벤트 생성
        CalendarEvent calendarEvent = CalendarEvent.builder().userId(hostUser.getId()).title(savedMeeting.getTitle())
                .eventDate(savedMeeting.getScheduledAt().toLocalDate())
                .eventTime(savedMeeting.getScheduledAt().toLocalTime()).eventType(EventType.MEETING)
                .isImportant(savedMeeting.isImportant())
                .meeting(savedMeeting).build();

        calendarEventRepository.save(calendarEvent);

        return new MeetingCreateResponseDto(savedMeeting);
    }

    // ==================================================================
    // 2. Read (조회)
    // ==================================================================
    public MeetingCreateResponseDto findById(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));
        return new MeetingCreateResponseDto(meeting);
    }

    public List<MeetingCreateResponseDto> getAllMeetings() {
        List<Meeting> meetings = meetingRepository.findAll();
        return meetings.stream().map(MeetingCreateResponseDto::new).collect(Collectors.toList());
    }

    public List<MeetingCreateResponseDto> findAllByHostUserId(Long hostUserId) {
        List<Meeting> meetings = meetingRepository.findAllByHostUser_IdOrderByScheduledAtDesc(hostUserId);
        return meetings.stream()
                .map(MeetingCreateResponseDto::new)
                .collect(Collectors.toList());
    }

    // ==================================================================
    // 3. Update (수정/종료)
    // ==================================================================
    
    // 회의 결과 업데이트 (제목, 요약, 키워드, 액션아이템 등)
    @Transactional
    public void updateMeetingResult(Long meetingId, MeetingUpdateResultDto updateDto) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        meeting.updateInfo(updateDto.getTitle(), null);

        // --- 참석자 처리 ---
        Map<String, String> existingNameIdMap = new HashMap<>();
        int maxSpeakerIndex = 0;

        for (Participant p : meeting.getParticipants()) {
            existingNameIdMap.put(p.getName(), p.getSpeakerId());
            if (p.getSpeakerId().startsWith("Speaker ")) {
                try {
                    int num = Integer.parseInt(p.getSpeakerId().replace("Speaker ", ""));
                    if (num > maxSpeakerIndex) maxSpeakerIndex = num;
                } catch (NumberFormatException e) {}
            }
        }

        if (updateDto.getParticipants() != null) {
            meeting.getParticipants().clear();
            participantRepository.flush();

            for (MeetingUpdateResultDto.ParticipantDto pDto : updateDto.getParticipants()) {
                String name = pDto.getName();
                String incomingId = pDto.getSpeakerId();
                String finalId;

                if (existingNameIdMap.containsKey(name)) {
                    finalId = existingNameIdMap.get(name);
                } else if (incomingId != null && incomingId.startsWith("Speaker ")) {
                    finalId = incomingId;
                    try {
                        int num = Integer.parseInt(finalId.replace("Speaker ", ""));
                        if (num > maxSpeakerIndex) maxSpeakerIndex = num;
                    } catch (Exception e) {}
                } else {
                    maxSpeakerIndex++;
                    finalId = "Speaker " + maxSpeakerIndex;
                }

                Participant p = Participant.builder()
                        .meeting(meeting)
                        .speakerId(finalId)
                        .name(name)
                        .build();
                meeting.getParticipants().add(p);
                
                existingNameIdMap.put(name, finalId);
            }
        }

        // --- MeetingResult 처리 ---
        MeetingResult meetingResult = meeting.getMeetingResult();
        if (meetingResult == null) {
            meetingResult = MeetingResult.builder().meeting(meeting).build();
            meeting.setMeetingResult(meetingResult);
        }

        ImportanceLevel importance = ImportanceLevel.MEDIUM;
        String importanceReason = "";
        if (updateDto.getImportance() != null) {
            if (updateDto.getImportance().getLevel() != null) {
                try {
                    String levelStr = mapToEnumString(updateDto.getImportance().getLevel());
                    importance = ImportanceLevel.valueOf(levelStr);
                } catch (Exception e) { }
            }
            importanceReason = updateDto.getImportance().getReason();
        }

        meetingResult.updateSummaryInfo(
                updateDto.getPurpose(),
                updateDto.getAgenda(),
                updateDto.getSummary(),
                importance,
                importanceReason
        );

        // --- 키워드 처리 ---
        if (updateDto.getKeywords() != null) {
            meetingResult.getKeywords().clear();
            meetingResultKeywordRepository.flush();
            
            Set<String> processedKeywords = new HashSet<>();

            for (MeetingUpdateResultDto.KeywordDto kDto : updateDto.getKeywords()) {
                String kName = kDto.getText().trim();
                if (processedKeywords.contains(kName)) continue;

                Keyword keyword = keywordRepository.findByName(kName)
                        .orElseGet(() -> keywordRepository.save(Keyword.builder().name(kName).build()));

                KeywordSource sourceEnum = KeywordSource.USER;
                if ("AI".equalsIgnoreCase(kDto.getSource())) sourceEnum = KeywordSource.AI;

                MeetingResultKeyword mrk = MeetingResultKeyword.builder()
                        .meetingResult(meetingResult)
                        .keyword(keyword)
                        .source(sourceEnum)
                        .build();
                
                meetingResult.getKeywords().add(mrk);
                processedKeywords.add(kName);
            }
        }

        // --- 액션 아이템 처리 ---
        if (updateDto.getActionItems() != null) {
            meetingResult.getActionItems().clear();
            meetingResultRepository.saveAndFlush(meetingResult);
            
            List<ActionItem> newActionItems = new ArrayList<>();
            
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
                        .isCompleted(itemDto.getIsCompleted() != null ? itemDto.getIsCompleted() : false)
                        .source(itemDto.getSource())
                        .build();
                
                newActionItems.add(actionItem);
            }
            actionItemRepository.saveAll(newActionItems);
            meetingResult.getActionItems().addAll(newActionItems);
        }

        // --- 발화 로그(Transcript) 처리 ---
        if (updateDto.getTranscripts() != null) {
            List<Transcript> currentTranscripts = meeting.getTranscripts();
            Map<Long, MeetingUpdateResultDto.TranscriptDto> incomingMap = new HashMap<>();
            List<MeetingUpdateResultDto.TranscriptDto> newItemsDto = new ArrayList<>();

            for (MeetingUpdateResultDto.TranscriptDto tDto : updateDto.getTranscripts()) {
                if (tDto.getId() != null) {
                    incomingMap.put(tDto.getId(), tDto);
                } else {
                    newItemsDto.add(tDto);
                }
            }

            currentTranscripts.removeIf(t -> {
                if (!incomingMap.containsKey(t.getId())) {
                    return true; 
                }
                MeetingUpdateResultDto.TranscriptDto dto = incomingMap.get(t.getId());
                
                String tSpeakerId = dto.getSpeaker();
                if (tSpeakerId == null || !tSpeakerId.startsWith("Speaker ")) {
                    if (existingNameIdMap.containsKey(dto.getSpeakerName())) {
                        tSpeakerId = existingNameIdMap.get(dto.getSpeakerName());
                    }
                }

                t.updateText(dto.getText());
                t.updateSpeaker(tSpeakerId, dto.getSpeakerName());
                t.updateSequenceOrder(dto.getSequenceOrder());

                if (Boolean.TRUE.equals(dto.getIsDeleted())) {
                    t.delete();
                } else {
                    t.restore();
                }
                return false;
            });

            for (MeetingUpdateResultDto.TranscriptDto tDto : newItemsDto) {
                String tSpeakerId = tDto.getSpeaker();
                if (existingNameIdMap.containsKey(tDto.getSpeakerName())) {
                    tSpeakerId = existingNameIdMap.get(tDto.getSpeakerName());
                } else if (tSpeakerId == null || !tSpeakerId.startsWith("Speaker ")) {
                    tSpeakerId = tDto.getSpeakerName(); 
                }

                Transcript newTranscript = Transcript.builder()
                        .meeting(meeting)
                        .speakerId(tSpeakerId)
                        .speakerName(tDto.getSpeakerName())
                        .text(tDto.getText())
                        .startTime(tDto.getStartTime() != null ? tDto.getStartTime() : 0L)
                        .endTime(tDto.getEndTime() != null ? tDto.getEndTime() : 0L)
                        .sequenceOrder(tDto.getSequenceOrder())
                        .isDeleted(Boolean.TRUE.equals(tDto.getIsDeleted()))
                        .build();
                currentTranscripts.add(newTranscript);
            }
        }
    }

    // 회의 종료 및 녹음/스크립트 저장
    @Transactional
    public void finishMeeting(Long meetingId, MeetingFinishRequestDto requestDto) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다. ID: " + meetingId));

        meeting.complete();

        if (requestDto.getRecording() != null) {
            MeetingFinishRequestDto.RecordingData recordingData = requestDto.getRecording();
            if (!recordingRepository.existsByMeetingId(meetingId)) {
                Recording recording = Recording.builder().meeting(meeting).audioFileUrl(recordingData.getAudioFileUrl())
                        .audioFileSize(recordingData.getAudioFileSize()).audioFormat(recordingData.getAudioFormat())
                        .durationSeconds(recordingData.getDurationSeconds()).build();
                recordingRepository.save(recording);
            }
        }

        if (requestDto.getTranscripts() != null && !requestDto.getTranscripts().isEmpty()) {
            if (transcriptRepository.existsByMeetingId(meetingId)) {
                transcriptRepository.deleteByMeetingId(meetingId);
            }
            List<Transcript> transcripts = requestDto.getTranscripts().stream()
                    .map(t -> Transcript.builder().meeting(meeting).speakerId(t.getSpeakerId())
                            .speakerName(t.getSpeakerName()).speakerLabel(t.getSpeakerLabel()).text(t.getText())
                            .startTime(t.getStartTime()).endTime(t.getEndTime()).sequenceOrder(t.getSequenceOrder())
                            .isDeleted(false).build())
                    .collect(Collectors.toList());
            transcriptRepository.saveAll(transcripts);
        }
        meetingRepository.save(meeting);
    }

    // ==================================================================
    // 4. Delete (삭제)
    // ==================================================================
    @Transactional
    public void deleteMeeting(Long meetingId) {
        meetingRepository.deleteById(meetingId);
    }

    // ==================================================================
    // 5. AI Logic (요약, 액션생성)
    // ==================================================================
    @Transactional
    public MeetingResult generateAISummary(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다."));

        List<Transcript> transcripts = transcriptRepository.findByMeetingIdOrderBySequenceOrder(meetingId);
        if (transcripts.isEmpty()) {
            throw new IllegalArgumentException("요약할 대화 내용이 없습니다.");
        }

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
            AISummaryResponse aiResponse = restTemplate.postForObject(pythonEndpoint, requestPayload,
                    AISummaryResponse.class);

            if (aiResponse != null && aiResponse.isSuccess() && aiResponse.getSummary() != null) {
                AISummaryResponse.AISummaryData data = aiResponse.getSummary();

                MeetingResult meetingResult = meeting.getMeetingResult();
                if (meetingResult == null) {
                    meetingResult = MeetingResult.builder().meeting(meeting).build();
                    meetingResult = meetingResultRepository.save(meetingResult);
                }

                ImportanceLevel level = (data.getImportance() != null) ? data.getImportance() : ImportanceLevel.MEDIUM;

                meetingResult.updateSummaryInfo(data.getPurpose(), data.getAgenda(), data.getOverallSummary(), level, "");

                List<String> aiKeywords = data.getKeywords();
                if (aiKeywords != null) {
                    Set<String> existingNames = new HashSet<>();
                    if (meetingResult.getKeywords() != null) {
                        meetingResult.getKeywords()
                                .forEach(mrk -> existingNames.add(mrk.getKeyword().getName().trim()));
                    }

                    for (String kName : aiKeywords) {
                        String normalizedName = kName.trim();
                        if (existingNames.contains(normalizedName)) continue;

                        Keyword keyword = keywordRepository.findByName(normalizedName).orElseGet(
                                () -> keywordRepository.save(Keyword.builder().name(normalizedName).build()));

                        MeetingResultKeyword mrk = MeetingResultKeyword.builder().meetingResult(meetingResult)
                                .keyword(keyword).source(KeywordSource.AI).build();

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
            throw new RuntimeException("AI 요약 생성에 실패했습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateAllActions(Long meetingId, Map<String, Object> requestData) {
        String pythonEndpoint = fastApiBaseUrl + "/actions/generate";
        try {
            Map<String, Object> aiResponse = restTemplate.postForObject(pythonEndpoint, requestData, Map.class);
            if (aiResponse != null) {
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

    // ==================================================================
    // 6. Helper
    // ==================================================================
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