package com.traffic.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "Kakaousers")
public class KakaoUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private KakaoUserRole role;

    private Long kakaoId;

    public KakaoUser(String username, String password, String email, KakaoUserRole role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public KakaoUser(String username, String password, String email, KakaoUserRole role, Long kakaoId) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.kakaoId =kakaoId;
    }

    public KakaoUser kakaoIdUpdate(Long kakaoId) {
        this.kakaoId = kakaoId;
        return this;
    }
}