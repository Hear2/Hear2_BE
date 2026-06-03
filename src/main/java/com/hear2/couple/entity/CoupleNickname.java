package com.hear2.couple.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "couple_nickname",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_couple_nickname_couple_giver",
                        columnNames = {"couple_id", "giver_user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoupleNickname {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "couple_nickname_id")
    private Long coupleNicknameId;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "giver_user_id", nullable = false)
    private Long giverUserId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
