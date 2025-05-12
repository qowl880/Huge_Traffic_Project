package com.traffic.userservice.repository;

import com.traffic.userservice.model.KakaoUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KakaoUserRepository extends JpaRepository<KakaoUser, Long> {

    Optional<KakaoUser> findByKakaoId(Long kakaoUserId);
    Optional<KakaoUser> findByEmail(String email);
}
