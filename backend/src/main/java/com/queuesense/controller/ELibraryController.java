package com.queuesense.controller;

import com.queuesense.dto.ELibraryFloorPlanDto;
import com.queuesense.dto.ELibrarySeatDto;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.service.ELibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elibrary")
public class ELibraryController {

    private final ELibraryService eLibraryService;

    public ELibraryController(ELibraryService eLibraryService) {
        this.eLibraryService = eLibraryService;
    }

    @GetMapping("/floorplan")
    public ResponseEntity<ELibraryFloorPlanDto> getFloorPlan(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(eLibraryService.getFloorPlan(email));
    }

    @GetMapping("/seats")
    public ResponseEntity<List<ELibrarySeatDto>> getSeats(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        ELibraryFloorPlanDto floorPlan = eLibraryService.getFloorPlan(email);
        return ResponseEntity.ok(floorPlan.getSeats());
    }

    @GetMapping("/seats/{seatNumber}")
    public ResponseEntity<ELibrarySeatDto> getSeatByNumber(@PathVariable Integer seatNumber,
                                                           Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        ELibraryFloorPlanDto floorPlan = eLibraryService.getFloorPlan(email);
        ELibrarySeatDto found = floorPlan.getSeats().stream()
                .filter(s -> s.getSeatNumber().equals(seatNumber))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Workstation #" + seatNumber + " not found in E-Library"));
        return ResponseEntity.ok(found);
    }
}
