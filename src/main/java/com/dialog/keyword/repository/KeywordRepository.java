package com.dialog.keyword.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.dialog.keyword.domain.Keyword;
import com.dialog.user.domain.MeetUser;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    List<Keyword> findByMeetingsId(Long meetingId);

    Optional<Keyword> findByName(String name);
}