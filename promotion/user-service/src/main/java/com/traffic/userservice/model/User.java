package com.traffic.userservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    @JsonIgnore         // 이 컬럼의 값을 JSON으로 변환할 때 무시
    private String password;

    // columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    // 데이터베이스에서 이 컬럼의 타입을 DATETIME으로 설정하고, 기본값으로 현재 시각을 사용하도록 합니다.
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp      // 현재 시각을 자동으로 입력
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public User KakaoToUser(KakaoUser kakaoUser){
        this.name = kakaoUser.getUsername();
        this.email = kakaoUser.getEmail();
        this.password = kakaoUser.getPassword();
        return this;
    }
}
