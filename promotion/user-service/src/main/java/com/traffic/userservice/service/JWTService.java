package com.traffic.userservice.service;

import com.traffic.userservice.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@RequiredArgsConstructor
@Slf4j
@Service
public class JWTService {
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secreteKey;


    // 토큰 생성
    public String generateToken(User user) {
        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role","USER")
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000))  // 1시간
                .signWith(Keys.hmacShaKeyFor(secreteKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
    
    // 토큰 검증
    public Claims validateToken(String token){
        try{
            return parseJwtClaims(token);
        }catch (Exception e){
            log.error("Token validation failed", e);
            throw new IllegalArgumentException("Invalid token");
        }
    }
    
    // 토큰 분해
    private Claims parseJwtClaims(String token){
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secreteKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Refresh 토큰 생성ㄴ
    public String refreshToken(String token){
        Claims claims = parseJwtClaims(token);
        long currentTimeMillis = System.currentTimeMillis();

        return Jwts.builder()
                .subject(claims.getSubject())
                .claims(claims)
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000))  // 1시간
                .signWith(Keys.hmacShaKeyFor(secreteKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
