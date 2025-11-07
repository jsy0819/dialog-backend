package com.dialog.user.service;

import java.util.List;

import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.participant.repository.ParticipantRepository;
import com.dialog.token.repository.RefreshTokenRepository;
import com.dialog.user.domain.AdminResponse;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.domain.MeetUserDto;
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
	    // 1. 삭제할 사용자 객체를 조회합니다.
	    MeetUser user = meetUserRepository.findById(userId)
	        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다. id=" + userId));
	    
	    participantRepository.deleteBySpeakerId(user.getEmail()); 

	    meetingRepository.deleteByHostUser(user); 

	    refreshTokenRepository.deleteByUser(user);
	    
	    meetUserRepository.delete(user);
	}
	@Transactional
    public void updateUserSettings(Long userId, UserSettingsUpdateDto updateDto) {
        MeetUser user = meetUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다. id=" + userId));

        // DTO에 Job과 Position Enum이 있으므로 그대로 set 호출
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
}
