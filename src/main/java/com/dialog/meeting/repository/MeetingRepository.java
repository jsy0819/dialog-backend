package com.dialog.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.meeting.domain.Meeting;
import com.dialog.user.domain.MeetUser;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	void deleteByHostUser(MeetUser user);

}
