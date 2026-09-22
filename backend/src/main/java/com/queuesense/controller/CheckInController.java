package com.queuesense.controller;

import com.queuesense.dto.UsageRequestResponseDto;
import com.queuesense.dto.VerifyCheckInDto;
import com.queuesense.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/check-in")
@PreAuthorize("hasRole('OPERATOR')")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/verify")
    public ResponseEntity<UsageRequestResponseDto> verifyAndCheckIn(@Valid @RequestBody VerifyCheckInDto verifyDto,
                                                                   Authentication authentication) {
        UsageRequestResponseDto response = checkInService.verifyAndCheckIn(verifyDto, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
