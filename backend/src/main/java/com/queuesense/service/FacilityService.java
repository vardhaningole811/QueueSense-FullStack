package com.queuesense.service;

import com.queuesense.entity.Facility;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.FacilityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityService(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    public List<Facility> getAllActiveFacilities() {
        return facilityRepository.findByActiveTrue();
    }

    public Facility getFacilityById(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));
    }
}
