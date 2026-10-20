package com.queuesense.service;

import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.FacilityRepository;
import com.queuesense.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final FacilityRepository facilityRepository;
    private final AuditLogService auditLogService;
    private final BookingLifecycleService bookingLifecycleService;

    public ResourceService(ResourceRepository resourceRepository,
                           FacilityRepository facilityRepository,
                           AuditLogService auditLogService,
                           BookingLifecycleService bookingLifecycleService) {
        this.resourceRepository = resourceRepository;
        this.facilityRepository = facilityRepository;
        this.auditLogService = auditLogService;
        this.bookingLifecycleService = bookingLifecycleService;
    }

    public List<Resource> getResourcesByFacility(Long facilityId) {
        bookingLifecycleService.processExpiredBookings(bookingLifecycleService.getCurrentTime());
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + facilityId));
        return resourceRepository.findByFacilityAndActiveTrue(facility);
    }

    public List<Resource> getAllResources() {
        bookingLifecycleService.processExpiredBookings(bookingLifecycleService.getCurrentTime());
        return resourceRepository.findAll();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
    }

    @Transactional
    public Resource updateResourceStatus(Long id, ResourceStatus newStatus, String operatorEmail) {
        Resource resource = getResourceById(id);
        ResourceStatus oldStatus = resource.getStatus();
        resource.setStatus(newStatus);
        Resource saved = resourceRepository.save(resource);

        auditLogService.log(operatorEmail, "ROLE_OPERATOR", "RESOURCE_STATUS_CHANGED", "Resource", id.toString(),
                "Changed status from " + oldStatus + " to " + newStatus);

        return saved;
    }
}
