package com.hear2.location.repository;

import com.hear2.location.entity.LocationShareSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationShareSettingRepository extends JpaRepository<LocationShareSetting, Long> {

    Optional<LocationShareSetting> findByCoupleIdAndUserId(Long coupleId, Long userId);

    List<LocationShareSetting> findByCoupleIdAndEnabledTrue(Long coupleId);
}
