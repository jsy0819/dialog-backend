package com.dialog.meeting.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.meeting.domain.Meeting;
// 👈 [수정] DTO 패키지 경로로 변경
import com.dialog.meeting.domain.MeetingCreateRequestDto;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회의(Meeting) 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
@Service
@RequiredArgsConstructor // final 레포지토리 필드 생성자 주입
@Transactional(readOnly = true) // (기본) 읽기 전용 트랜잭션 (조회 성능 최적화)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetUserRepository meetUserRepository; 

    /**
     * 새로운 회의를 생성합니다. (쓰기 작업)
     */
    @Transactional // 이 메서드는 DB에 쓰기 작업을 하므로 별도 @Transactional 명시
    public MeetingCreateResponseDto createMeeting(MeetingCreateRequestDto requestDto, Long hostUserId) {

        // 1. 주최자(User) 엔티티 조회
        MeetUser hostUser = meetUserRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")); // .get() 대신 예외 처리

        // 2. 👈 [수정] 빌더 패턴을 사용해 DTO를 Meeting 엔티티로 변환
        Meeting newMeeting = Meeting.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .scheduledAt(requestDto.getScheduledAt())
                .hostUser(hostUser)
                // .status(Status.SCHEDULED) // 엔티티의 @Builder.Default로 자동 설정됨
                .build();

        // 3. (저장) 레포지토리를 사용해 엔티티를 DB에 저장 (영속화)
        Meeting savedMeeting = meetingRepository.save(newMeeting);

        // 4. (부가 로직) DTO로 받은 participantNames와 keywords를
        //    'Participant' 엔티티와 'Keyword' 엔티티로 변환하여 저장
        //    (TODO: 관련 서비스 로직 추가 위치)

        // 5. (변환) 저장된 엔티티를 Response DTO로 변환하여 컨트롤러에 반환
        return new MeetingCreateResponseDto(savedMeeting);
    }

    // TODO: 회의 조회, 수정, 삭제 등의 다른 서비스 메소드들...
}