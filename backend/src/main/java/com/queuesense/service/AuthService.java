package com.queuesense.service;

import com.queuesense.dto.AuthResponse;
import com.queuesense.dto.LoginRequest;
import com.queuesense.entity.OperatorProfile;
import com.queuesense.entity.StudentProfile;
import com.queuesense.entity.User;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.OperatorProfileRepository;
import com.queuesense.repository.StudentProfileRepository;
import com.queuesense.repository.UserRepository;
import com.queuesense.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OperatorProfileRepository operatorProfileRepository;
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserRepository userRepository,
                       StudentProfileRepository studentProfileRepository,
                       OperatorProfileRepository operatorProfileRepository,
                       AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.operatorProfileRepository = operatorProfileRepository;
        this.auditLogService = auditLogService;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + loginRequest.getEmail()));

        AuthResponse response = new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().name());

        if (user.getRole().name().equals("ROLE_STUDENT")) {
            Optional<StudentProfile> studentProfile = studentProfileRepository.findByUser(user);
            studentProfile.ifPresent(profile -> response.setStudentId(profile.getStudentId()));
        } else if (user.getRole().name().equals("ROLE_OPERATOR")) {
            Optional<OperatorProfile> operatorProfile = operatorProfileRepository.findByUser(user);
            operatorProfile.ifPresent(profile -> {
                if (profile.getAssignedFacility() != null) {
                    response.setAssignedFacilityId(profile.getAssignedFacility().getId());
                    response.setAssignedFacilityName(profile.getAssignedFacility().getName());
                }
            });
        }

        auditLogService.log(user.getEmail(), user.getRole().name(), "LOGIN", "User", user.getId().toString(), "User logged in successfully");

        return response;
    }

    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        AuthResponse response = new AuthResponse(null, user.getId(), user.getEmail(), user.getName(), user.getRole().name());

        if (user.getRole().name().equals("ROLE_STUDENT")) {
            Optional<StudentProfile> studentProfile = studentProfileRepository.findByUser(user);
            studentProfile.ifPresent(profile -> response.setStudentId(profile.getStudentId()));
        } else if (user.getRole().name().equals("ROLE_OPERATOR")) {
            Optional<OperatorProfile> operatorProfile = operatorProfileRepository.findByUser(user);
            operatorProfile.ifPresent(profile -> {
                if (profile.getAssignedFacility() != null) {
                    response.setAssignedFacilityId(profile.getAssignedFacility().getId());
                    response.setAssignedFacilityName(profile.getAssignedFacility().getName());
                }
            });
        }

        return response;
    }
}
