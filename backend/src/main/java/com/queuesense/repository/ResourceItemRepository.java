package com.queuesense.repository;

import com.queuesense.entity.Resource;
import com.queuesense.entity.ResourceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceItemRepository extends JpaRepository<ResourceItem, Long> {
    List<ResourceItem> findByResourceAndActiveTrueOrderByUnitNumberAsc(Resource resource);
    List<ResourceItem> findByResourceIdAndActiveTrueOrderByUnitNumberAsc(Long resourceId);
    long countByResourceAndActiveTrue(Resource resource);
    long countByResourceAndStatusAndActiveTrue(Resource resource, String status);
    long countByResourceIdAndStatusAndActiveTrue(Long resourceId, String status);
    boolean existsByResource(Resource resource);
}
