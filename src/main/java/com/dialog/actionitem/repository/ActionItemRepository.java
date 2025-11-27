package com.dialog.actionitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.actionitem.domain.ActionItem;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

}
