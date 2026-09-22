package com.queuesense.repository;

import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.enums.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByFacilityAndActiveTrue(Facility facility);
    List<Resource> findByFacilityIdAndActiveTrue(Long facilityId);
    List<Resource> findByStatus(ResourceStatus status);
    long countByStatus(ResourceStatus status);
    long countByFacilityIdAndStatus(Long facilityId, ResourceStatus status);
}
