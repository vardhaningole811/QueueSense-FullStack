package com.queuesense.repository;

import com.queuesense.entity.Facility;
import com.queuesense.entity.OperatorProfile;
import com.queuesense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorProfileRepository extends JpaRepository<OperatorProfile, Long> {
    Optional<OperatorProfile> findByUser(User user);
    Optional<OperatorProfile> findByEmployeeId(String employeeId);
    List<OperatorProfile> findByAssignedFacility(Facility facility);
}
