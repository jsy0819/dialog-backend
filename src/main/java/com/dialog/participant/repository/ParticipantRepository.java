package com.dialog.participant.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.dialog.participant.domain.Participant;
import com.dialog.user.domain.MeetUser;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByMeetingId(Long meetingId);

    void deleteBySpeakerId(String speakerId);    
    void deleteByMeetingId(Long meetingId);
}