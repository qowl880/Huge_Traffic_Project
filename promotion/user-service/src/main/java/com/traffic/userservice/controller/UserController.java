package com.traffic.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.traffic.userservice.exception.DuplicateUserException;
import com.traffic.userservice.exception.UnauthorizedAccessException;
import com.traffic.userservice.exception.UserNotFoundException;
import com.traffic.userservice.model.User;
import com.traffic.userservice.model.UserLoginHistory;
import com.traffic.userservice.model.dto.UserDto;
import com.traffic.userservice.service.KakaoService;
import com.traffic.userservice.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final KakaoService kakaoService;

    @PostMapping("/signup")
    public ResponseEntity<?> createUser(
            @RequestBody UserDto.SignupRequest request) {
        User user = userService.createUser(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.Response.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(
            @RequestHeader("X-USER-ID") Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UserDto.UpdateRequest request) {
        User user = userService.updateUser(userId, request.getName());
        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UserDto.PasswordChangeRequest request) {
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/login-history")
    public ResponseEntity<List<UserLoginHistory>> getLoginHistory(
            @RequestHeader("X-USER-ID") Long userId) {
        List<UserLoginHistory> history = userService.getUserLoginHistory(userId);
        return ResponseEntity.ok(history);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<String> handleDuplicateUser(DuplicateUserException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> handleUnauthorizedAccess(UnauthorizedAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @GetMapping("/kakao/callback")
    public String kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        // JWT 반환
        String token = kakaoService.kakaoLogin(code);

        Cookie cookie = new Cookie("Authorization", token.substring(7));
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/";
    }
}
