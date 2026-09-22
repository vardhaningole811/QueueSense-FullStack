package com.queuesense.controller;

import com.queuesense.entity.Resource;
import com.queuesense.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<List<Resource>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<Resource>> getResourcesByFacility(@PathVariable Long facilityId) {
        return ResponseEntity.ok(resourceService.getResourcesByFacility(facilityId));
    }
}
