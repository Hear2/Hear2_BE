package com.hear2.whatif.repository;

import com.hear2.whatif.entity.WhatIfHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WhatIfHistoryRepository extends JpaRepository<WhatIfHistory, Long> {

    List<WhatIfHistory> findTop30ByCoupleIdOrderByCreatedAtDesc(Long coupleId);

    Optional<WhatIfHistory> findByIdAndCoupleId(Long id, Long coupleId);
}
