package com.queuesense.repository;

import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.enums.ResourceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByFacilityAndActiveTrue(Facility facility);
    List<Resource> findByFacilityIdAndActiveTrue(Long facilityId);
    List<Resource> findByFacilityAndActiveTrueOrderByIdAsc(Facility facility);
    Optional<Resource> findByFacilityAndName(Facility facility, String name);
    Optional<Resource> findByFacilityAndNameIgnoreCase(Facility facility, String name);
    List<Resource> findByStatus(ResourceStatus status);
    long countByStatus(ResourceStatus status);
    long countByFacilityIdAndStatus(Long facilityId, ResourceStatus status);

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.active = true AND UPPER(r.facility.type) <> 'SPORTS' AND r.status = :status")
    long countNonSportsByStatus(@Param("status") ResourceStatus status);

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.active = true AND UPPER(r.facility.type) <> 'SPORTS'")
    long countNonSportsTotal();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}
