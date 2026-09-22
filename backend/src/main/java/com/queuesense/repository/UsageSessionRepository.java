package com.queuesense.repository;

import com.queuesense.entity.UsageRequest;
import com.queuesense.entity.UsageSession;
import com.queuesense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageSessionRepository extends JpaRepository<UsageSession, Long> {
    Optional<UsageSession> findByUsageRequest(UsageRequest usageRequest);
    List<UsageSession> findByStudentOrderByStartedAtDesc(User student);
    List<UsageSession> findByOrderByStartedAtDesc();
}
