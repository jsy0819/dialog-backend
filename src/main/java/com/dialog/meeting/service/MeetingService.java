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
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.participant.domain.Participant;
import com.dialog.participant.repository.ParticipantRepository;
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

    // 회의 생성
    @Transactional 
    public MeetingCreateResponseDto createMeeting(MeetingCreateRequestDto requestDto, Long hostUserId) throws IllegalAccessException {

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
        Meeting newMeeting = Meeting.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .scheduledAt(scheduledAt)
                .hostUser(hostUser)
                .build();

        // 3. 엔티티를 DB에 저장
        Meeting savedMeeting = meetingRepository.save(newMeeting);

        // 4. 참석자 등록
        List<Participant> participantEntities = new ArrayList<>();
        for (String speakerId : requestDto.getParticipants()) {
            Participant participant = Participant.builder()
                .meeting(savedMeeting)
                .speakerId(speakerId)
                .name(speakerId) 
                .build();
            participantEntities.add(participant);
        }
        participantRepository.saveAll(participantEntities);
        
        // 5. 키워드 등록 및 ManyToMany 연관관계 설정
        // 기존 키워드 재사용(중복사용방지) , 없으면 새로 생성하여 저장
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
	    return meetings.stream()
	            .map(meeting -> {
	                List<String> participantNames = meeting.getParticipants().stream()
	                        .map(Participant::getName) // 또는 Participant::getSpeakerId
	                        .collect(Collectors.toList());

	                List<String> keywordTexts = meeting.getKeywords().stream()
	                        .map(Keyword::getName)
	                        .collect(Collectors.toList());

	                return new MeetingCreateResponseDto(meeting, participantNames, keywordTexts);
	            })
	            .collect(Collectors.toList());
	}

}