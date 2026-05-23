package com.hear2.character.repository;

import com.hear2.character.entity.CharacterExpHistory;
import com.hear2.character.support.CharacterExpSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface CharacterExpHistoryRepository extends JpaRepository<CharacterExpHistory, Long> {

    boolean existsBySourceTypeAndSourceId(CharacterExpSourceType sourceType, String sourceId);

    @Query("""
            select coalesce(sum(history.expAmount), 0)
            from CharacterExpHistory history
            where history.coupleId = :coupleId
              and history.earnedDate = :earnedDate
            """)
    long sumExpAmountByCoupleIdAndEarnedDate(
            @Param("coupleId") Long coupleId,
            @Param("earnedDate") LocalDate earnedDate
    );

    @Query("""
            select coalesce(sum(history.expAmount), 0)
            from CharacterExpHistory history
            where history.coupleId = :coupleId
              and history.earnedDate = :earnedDate
              and history.sourceType = :sourceType
            """)
    long sumExpAmountByCoupleIdAndEarnedDateAndSourceType(
            @Param("coupleId") Long coupleId,
            @Param("earnedDate") LocalDate earnedDate,
            @Param("sourceType") CharacterExpSourceType sourceType
    );
}
