package com.dialog.actionitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.actionitem.domain.ActionItem;

import java.util.Optional;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    Optional<ActionItem> findByGoogleEventId(String googleEventId);
}
