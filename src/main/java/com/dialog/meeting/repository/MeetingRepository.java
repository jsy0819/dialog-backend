package com.dialog.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.meeting.domain.Meeting;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

}
