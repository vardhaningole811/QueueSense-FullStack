package com.queuesense.controller;

import com.queuesense.dto.DashboardStatsDto;
import com.queuesense.dto.RejectRequestDto;
import com.queuesense.dto.UsageRequestResponseDto;
import com.queuesense.entity.Resource;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.service.DashboardService;
import com.queuesense.service.ResourceService;
import com.queuesense.service.UsageRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator")
@PreAuthorize("hasRole('OPERATOR')")
public class OperatorController {

    private final DashboardService dashboardService;
    private final UsageRequestService usageRequestService;
    private final ResourceService resourceService;

    public OperatorController(DashboardService dashboardService,
                              UsageRequestService usageRequestService,
                              ResourceService resourceService) {
        this.dashboardService = dashboardService;
        this.usageRequestService = usageRequestService;
        this.resourceService = resourceService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<UsageRequestResponseDto>> getPendingRequests() {
        return ResponseEntity.ok(usageRequestService.getPendingRequests());
    }

    @GetMapping("/requests/approved")
    public ResponseEntity<List<UsageRequestResponseDto>> getApprovedRequests() {
        return ResponseEntity.ok(usageRequestService.getApprovedRequests());
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<UsageRequestResponseDto> approveRequest(@PathVariable Long id,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(usageRequestService.approveRequest(id, authentication.getName()));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<UsageRequestResponseDto> rejectRequest(@PathVariable Long id,
                                                                 @RequestBody(required = false) RejectRequestDto rejectDto,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(usageRequestService.rejectRequest(id, rejectDto, authentication.getName()));
    }

    @GetMapping("/resources")
    public ResponseEntity<List<Resource>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @PatchMapping("/resources/{id}/status")
    public ResponseEntity<Resource> updateResourceStatus(@PathVariable Long id,
                                                         @RequestBody Map<String, String> payload,
                                                         Authentication authentication) {
        ResourceStatus newStatus = ResourceStatus.valueOf(payload.get("status"));
        return ResponseEntity.ok(resourceService.updateResourceStatus(id, newStatus, authentication.getName()));
    }
}
