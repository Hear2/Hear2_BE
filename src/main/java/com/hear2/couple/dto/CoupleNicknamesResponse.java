package com.hear2.couple.dto;

import com.hear2.couple.entity.CoupleNickname;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CoupleNicknamesResponse {

    private Map<Long, String> byGiver;

    public static CoupleNicknamesResponse from(List<CoupleNickname> nicknames) {
        Map<Long, String> byGiver = new LinkedHashMap<>();
        if (nicknames != null) {
            nicknames.forEach(nickname -> byGiver.put(nickname.getGiverUserId(), nickname.getNickname()));
        }

        return CoupleNicknamesResponse.builder()
                .byGiver(byGiver)
                .build();
    }
}
