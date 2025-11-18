package com.dialog.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.dialog.exception.ResourceNotFoundException;
import com.dialog.exception.UserNotFoundException;
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.participant.repository.ParticipantRepository;
import com.dialog.token.repository.RefreshTokenRepository;
import com.dialog.user.domain.AdminResponse;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.domain.TodayStatsDto;
import com.dialog.user.domain.UserSettingsUpdateDto;
import com.dialog.user.repository.MeetUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final MeetUserRepository meetUserRepository;
	private final ParticipantRepository participantRepository;
	private final MeetingRepository meetingRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	
	@Transactional(readOnly = true)
	public List<AdminResponse> getAllUsers() {
		return meetUserRepository.findAll().stream().map(AdminResponse::from) // 엔티티를 Admin 전용 DTO로 변환
				.toList();
	}

	@Transactional
	public void deleteUser(Long userId) {
	    MeetUser user = meetUserRepository.findById(userId)
	        .orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다. id=" + userId));

	    participantRepository.deleteBySpeakerId(user.getEmail()); 
	    meetingRepository.deleteByHostUser(user); 
	    refreshTokenRepository.deleteByUser(user);
	    meetUserRepository.delete(user);
	}
	
	@Transactional
	public void updateUserSettings(Long userId, UserSettingsUpdateDto updateDto) {
	    MeetUser user = meetUserRepository.findById(userId)
	        .orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다. id=" + userId));

	    if (updateDto.getJob() != null) {
	        user.setJob(updateDto.getJob());
	    }
	    if (updateDto.getPosition() != null) {
	        user.setPosition(updateDto.getPosition());
	    }
	    if (updateDto.getActive() != null) {
	        user.setActive(updateDto.getActive());
	    }
	}
	
	// 가입한 유저수 조회
    public long getTotalUserCount() {
        return meetUserRepository.count();
    }
    
    // 7일 이내 새로 가입한 유저수 조회
    public int getNewUserCountLast7Days() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return meetUserRepository.countByCreatedAtAfter(sevenDaysAgo);
    }
    
    // 생성한 회의 수 조회
    public long getTotalMeetingCount() {
        return meetingRepository.count();
    }
    
    // 이번달 생성한 회의 수 조회
    public long getMeetingCountThisMonth() {
        YearMonth thisMonth = YearMonth.now();
        LocalDateTime start = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime end = thisMonth.atEndOfMonth().atTime(23, 59, 59);
        return meetingRepository.countMeetingsInMonth(start, end);
    }

    // 오늘 가입한 사용자 조회
    public long countTodayRegisteredUsers() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1).minusNanos(1);
        return meetUserRepository.countTodayRegisteredUsers(start, end);
    }

    // 오늘 생성한 회의 조회
    public long countTodayCreatedMeetings() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1).minusNanos(1);
        return meetingRepository.countTodayCreatedMeetings(start, end);
    }

    // 어제 가입한 사용자 조회
    public long countYesterdayRegisteredUsers() {
        LocalDateTime start = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime end = start.plusDays(1).minusNanos(1);
        return meetUserRepository.countYesterdayRegisteredUsers(start, end);
    }

    // 어제 생성한 회의 조회
    public long countYesterdayCreatedMeetings() {
        LocalDateTime start = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime end = start.plusDays(1).minusNanos(1);
        return meetingRepository.countYesterdayCreatedMeetings(start, end);
    }

    // 어제 가입,생성한 회의 - 오늘 가입,생성한 사용자,회의 값 조회  
    public TodayStatsDto getTodayStats() {
        long todayMeetCount = countTodayCreatedMeetings();
        long yesterdayMeetCount = countYesterdayCreatedMeetings();
        long todayUserCount = countTodayRegisteredUsers();
        long yesterdayUserCount = countYesterdayRegisteredUsers();

        long meetChange = todayMeetCount - yesterdayMeetCount;
        long userChange = todayUserCount - yesterdayUserCount;

        return new TodayStatsDto(todayMeetCount, meetChange, todayUserCount, userChange);
    }
   
    @Transactional
	public void deleteMeeting(Long meetingId) {
    	if (!meetingRepository.existsById(meetingId)) {
	        throw new ResourceNotFoundException("해당 회의가 존재하지 않습니다. id=" + meetingId);
	    }
	  
    	participantRepository.deleteByMeetingId(meetingId); 
    	meetingRepository.deleteById(meetingId);
	}
}