package com.queuesense.repository;

import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.UsageRequest;
import com.queuesense.entity.User;
import com.queuesense.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageRequestRepository extends JpaRepository<UsageRequest, Long> {
    Optional<UsageRequest> findByRequestCode(String requestCode);
    List<UsageRequest> findByStudentOrderByCreatedAtDesc(User student);
    List<UsageRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<UsageRequest> findByFacilityAndStatusOrderByCreatedAtDesc(Facility facility, RequestStatus status);
    List<UsageRequest> findByResourceAndStatus(Resource resource, RequestStatus status);
    boolean existsByStudentAndStatusIn(User student, List<RequestStatus> statuses);
    boolean existsByStudentAndResourceAndStatusIn(User student, Resource resource, List<RequestStatus> statuses);
    long countByStatus(RequestStatus status);
    long countByFacilityIdAndStatus(Long facilityId, RequestStatus status);
}
