package com.queuesense.controller;

import com.queuesense.entity.UsageSession;
import com.queuesense.entity.User;
import com.queuesense.repository.UsageSessionRepository;
import com.queuesense.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final UsageSessionRepository usageSessionRepository;
    private final UserRepository userRepository;

    public HistoryController(UsageSessionRepository usageSessionRepository, UserRepository userRepository) {
        this.usageSessionRepository = usageSessionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UsageSession>> getHistory(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_STUDENT".equals(role)) {
            User student = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (student == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(usageSessionRepository.findByStudentOrderByStartedAtDesc(student));
        } else {
            return ResponseEntity.ok(usageSessionRepository.findByOrderByStartedAtDesc());
        }
    }
}
