package com.hear2.location.repository;

import com.hear2.location.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    Optional<UserLocation> findByCoupleIdAndUserId(Long coupleId, Long userId);

    List<UserLocation> findByCoupleIdAndUserIdNotOrderByUpdatedAtDesc(Long coupleId, Long userId);
}
