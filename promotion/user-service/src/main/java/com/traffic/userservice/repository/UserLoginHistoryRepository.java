package com.traffic.userservice.repository;

import com.traffic.userservice.model.User;
import com.traffic.userservice.model.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {
    List<UserLoginHistory> findByUserOrderByLoginTimeDesc(User user);
}
