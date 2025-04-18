package com.traffic.userservice.service;

import com.netflix.discovery.converters.Auto;
import com.traffic.userservice.exception.DuplicateUserException;
import com.traffic.userservice.exception.UnauthorizedAccessException;
import com.traffic.userservice.exception.UserNotFoundException;
import com.traffic.userservice.model.User;
import com.traffic.userservice.model.UserLoginHistory;
import com.traffic.userservice.repository.UserLoginHistoryRepository;
import com.traffic.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String email, String password, String name) {
        if(userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("User already exists with email:"+email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);

        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedAccessException("Invalid credentials");
        }

        return user;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Transactional
    public User updateUser(Long userId, String name){
        User user = getUserById(userId);
        user.setName(name);
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UnauthorizedAccessException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserLoginHistory> getUserLoginHistory(Long userId) {
        User user = getUserById(userId);
        return userLoginHistoryRepository.findByUserOrderByLoginTimeDesc(user);
    }

}
