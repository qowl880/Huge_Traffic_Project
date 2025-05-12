package com.traffic.pointservicebatch.repository;

import com.traffic.pointservicebatch.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepository extends JpaRepository<Point, Long> {
}
