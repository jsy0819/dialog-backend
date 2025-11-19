package com.dialog.calendarevent.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.calendarevent.domain.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long> {

}
