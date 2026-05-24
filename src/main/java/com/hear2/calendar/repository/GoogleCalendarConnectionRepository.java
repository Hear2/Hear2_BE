package com.hear2.calendar.repository;

import com.hear2.calendar.entity.GoogleCalendarConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, Long> {

    Optional<GoogleCalendarConnection> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}
