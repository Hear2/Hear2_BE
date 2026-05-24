package com.hear2.calendar.repository;

import com.hear2.calendar.entity.GoogleCalendarOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarOAuthStateRepository extends JpaRepository<GoogleCalendarOAuthState, Long> {

    Optional<GoogleCalendarOAuthState> findByState(String state);
}
