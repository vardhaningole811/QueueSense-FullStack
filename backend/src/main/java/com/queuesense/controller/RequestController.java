package com.queuesense.controller;

import com.queuesense.dto.CreateUsageRequestDto;
import com.queuesense.dto.UsageRequestResponseDto;
import com.queuesense.service.CheckOutService;
import com.queuesense.service.UsageRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final UsageRequestService usageRequestService;
    private final CheckOutService checkOutService;

    public RequestController(UsageRequestService usageRequestService, CheckOutService checkOutService) {
        this.usageRequestService = usageRequestService;
        this.checkOutService = checkOutService;
    }

    @PostMapping
    public ResponseEntity<UsageRequestResponseDto> createRequest(@Valid @RequestBody CreateUsageRequestDto dto,
                                                                 Authentication authentication) {
        UsageRequestResponseDto response = usageRequestService.createRequest(dto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<List<UsageRequestResponseDto>> getMyRequests(Authentication authentication) {
        List<UsageRequestResponseDto> requests = usageRequestService.getStudentRequests(authentication.getName());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsageRequestResponseDto> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(usageRequestService.getRequestById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<UsageRequestResponseDto> getRequestByCode(@PathVariable String code) {
        return ResponseEntity.ok(usageRequestService.getRequestByCode(code));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<UsageRequestResponseDto> cancelRequest(@PathVariable Long id,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(usageRequestService.cancelRequest(id, authentication.getName()));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<UsageRequestResponseDto> checkOut(@PathVariable Long id,
                                                            Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(checkOutService.checkOut(id, authentication.getName(), role));
    }
}
