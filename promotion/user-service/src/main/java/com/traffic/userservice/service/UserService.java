package com.traffic.userservice.service;

import com.netflix.discovery.converters.Auto;
import com.traffic.userservice.exception.DuplicateUserException;
import com.traffic.userservice.model.User;
import com.traffic.userservice.repository.UserLoginHistoryRepository;
import com.traffic.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
                .orElseThrow(null);
    }
}
