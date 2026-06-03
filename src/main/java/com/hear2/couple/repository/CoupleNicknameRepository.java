package com.hear2.couple.repository;

import com.hear2.couple.entity.CoupleNickname;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoupleNicknameRepository extends JpaRepository<CoupleNickname, Long> {

    List<CoupleNickname> findByCoupleIdOrderByGiverUserIdAsc(Long coupleId);

    Optional<CoupleNickname> findByCoupleIdAndGiverUserId(Long coupleId, Long giverUserId);
}
