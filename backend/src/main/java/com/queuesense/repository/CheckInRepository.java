package com.queuesense.repository;

import com.queuesense.entity.CheckIn;
import com.queuesense.entity.UsageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    Optional<CheckIn> findByUsageRequest(UsageRequest usageRequest);

    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.checkedInAt >= :startOfDay")
    long countTodayCheckIns(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.checkedInAt >= :startOfDay AND c.checkedInAt < :startOfNextDay")
    long countTodayCheckIns(@Param("startOfDay") LocalDateTime startOfDay, @Param("startOfNextDay") LocalDateTime startOfNextDay);
}
