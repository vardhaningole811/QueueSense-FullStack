package com.queuesense.repository;

import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.UsageRequest;
import com.queuesense.entity.User;
import com.queuesense.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    boolean existsByStudentAndFacilityAndStatusIn(User student, Facility facility, List<RequestStatus> statuses);
    boolean existsByStudentAndResourceAndStatusIn(User student, Resource resource, List<RequestStatus> statuses);
    List<UsageRequest> findByStudentAndStatusIn(User student, List<RequestStatus> statuses);
    List<UsageRequest> findByStudentAndFacilityAndStatusIn(User student, Facility facility, List<RequestStatus> statuses);
    List<UsageRequest> findByResourceAndStatusIn(Resource resource, List<RequestStatus> statuses);
    List<UsageRequest> findByResourceIdAndStatusIn(Long resourceId, List<RequestStatus> statuses);
    Optional<UsageRequest> findFirstByStudentAndFacilityAndStatusInOrderByCreatedAtDesc(User student, Facility facility, List<RequestStatus> statuses);
    long countByStatus(RequestStatus status);
    long countByFacilityIdAndStatus(Long facilityId, RequestStatus status);

    @Query("SELECT r FROM UsageRequest r WHERE r.resourceItem.id = :resourceItemId AND r.status IN :activeStatuses AND r.slotStartTime < :endTime AND r.slotEndTime > :startTime")
    List<UsageRequest> findOverlappingRequestsForItem(@Param("resourceItemId") Long resourceItemId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime,
                                                      @Param("activeStatuses") List<RequestStatus> activeStatuses);

    @Query("SELECT r FROM UsageRequest r WHERE r.resource.id = :resourceId AND r.status IN :activeStatuses AND r.slotStartTime < :endTime AND r.slotEndTime > :startTime")
    List<UsageRequest> findOverlappingRequestsForResource(@Param("resourceId") Long resourceId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime,
                                                          @Param("activeStatuses") List<RequestStatus> activeStatuses);

    List<UsageRequest> findByStatusAndExpiresAtLessThanEqual(RequestStatus status, LocalDateTime now);

    List<UsageRequest> findByStatusAndSlotEndTimeLessThanEqual(RequestStatus status, LocalDateTime now);

    List<UsageRequest> findByStatusInAndSlotEndTimeLessThanEqual(List<RequestStatus> statuses, LocalDateTime now);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM UsageRequest r WHERE UPPER(r.facility.type) = 'SPORTS' AND r.status = :status")
    long sumSportsQuantityByStatus(@Param("status") RequestStatus status);
}
