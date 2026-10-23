package com.queuesense.controller;

import com.queuesense.dto.analytics.CrowdAnalyticsResponseDto;
import com.queuesense.dto.analytics.CrowdSummaryDto;
import com.queuesense.service.CrowdAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class CrowdAnalyticsController {

    private final CrowdAnalyticsService crowdAnalyticsService;

    public CrowdAnalyticsController(CrowdAnalyticsService crowdAnalyticsService) {
        this.crowdAnalyticsService = crowdAnalyticsService;
    }

    @GetMapping("/facilities")
    public ResponseEntity<List<CrowdSummaryDto>> getAllFacilitiesCrowd() {
        return ResponseEntity.ok(crowdAnalyticsService.getAllFacilitiesCrowdSummary());
    }

    @GetMapping("/facilities/{facilityId}/crowd")
    public ResponseEntity<CrowdAnalyticsResponseDto> getFacilityCrowd(@PathVariable Long facilityId) {
        return ResponseEntity.ok(crowdAnalyticsService.getFacilityCrowdAnalytics(facilityId));
    }

    @GetMapping("/facilities/{facilityId}/forecast")
    public ResponseEntity<CrowdAnalyticsResponseDto> getFacilityForecast(@PathVariable Long facilityId) {
        return ResponseEntity.ok(crowdAnalyticsService.getFacilityCrowdAnalytics(facilityId));
    }
}
