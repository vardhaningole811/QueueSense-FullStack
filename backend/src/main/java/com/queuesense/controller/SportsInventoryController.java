package com.queuesense.controller;

import com.queuesense.dto.*;
import com.queuesense.service.SportsInventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sports")
public class SportsInventoryController {

    private final SportsInventoryService sportsInventoryService;

    public SportsInventoryController(SportsInventoryService sportsInventoryService) {
        this.sportsInventoryService = sportsInventoryService;
    }

    /**
     * Student-facing live availability for Sports Complex
     */
    @GetMapping("/student/availability")
    public ResponseEntity<SportsStudentAvailabilityResponseDto> getStudentAvailability(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(sportsInventoryService.getStudentAvailability(email));
    }

    /**
     * Live 1-hour time slots for courts and cricket nets (rolling 30-minute booking window)
     */
    @GetMapping("/student/court-slots")
    public ResponseEntity<List<CourtSlotDto>> getCourtSlots(
            @RequestParam(required = false) Long resourceId) {
        return ResponseEntity.ok(sportsInventoryService.getCourtSlots(resourceId, null));
    }

    /**
     * Administrator / Operator full inventory details
     */
    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<SportsInventoryDetailDto>> getAllInventoryDetails() {
        return ResponseEntity.ok(sportsInventoryService.getAllInventoryDetails());
    }

    /**
     * Increase or decrease total inventory of equipment, courts, or nets
     */
    @PostMapping("/inventory/{resourceId}/quantity")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<SportsResourceDto> updateTotalQuantity(
            @PathVariable Long resourceId,
            @Valid @RequestBody UpdateInventoryQuantityDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(sportsInventoryService.updateTotalQuantity(
                resourceId, dto.getNewTotalQuantity(), authentication.getName()));
    }

    /**
     * Update individual physical unit status (e.g. DAMAGED, MAINTENANCE, AVAILABLE)
     */
    @PatchMapping("/inventory/items/{itemId}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ResourceItemDto> updateItemStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemStatusDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(sportsInventoryService.updateItemStatus(
                itemId, dto, authentication.getName()));
    }

    /**
     * Temporarily close or reopen bookings for a specific resource
     */
    @PostMapping("/inventory/{resourceId}/toggle-closure")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<SportsResourceDto> toggleBookingClosure(
            @PathVariable Long resourceId,
            @RequestBody ToggleBookingClosureDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(sportsInventoryService.toggleBookingClosure(
                resourceId, dto.isClosed(), dto.getReason(), authentication.getName()));
    }
}
