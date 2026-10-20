package com.queuesense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuesense.dto.CreateUsageRequestDto;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.User;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.enums.Role;
import com.queuesense.repository.*;
import com.queuesense.security.JwtTokenProvider;
import com.queuesense.service.ELibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ELibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UsageRequestRepository usageRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ELibraryService eLibraryService;

    private String student1Token;
    private String student2Token;
    private User student1;
    private User student2;
    private Facility elibrary;

    @BeforeEach
    void setup() {
        student1 = userRepository.findByEmail("student@queuesense.demo").orElseThrow();

        student2 = userRepository.findByEmail("student2@queuesense.demo").orElseGet(() -> {
            User u = new User("student2@queuesense.demo", passwordEncoder.encode("Student@123"), Role.ROLE_STUDENT, "Pooja Patel");
            u = userRepository.save(u);
            studentProfileRepository.save(new com.queuesense.entity.StudentProfile(u, "STU-2026-002", "Information Technology", 2));
            return u;
        });

        // Cancel lingering requests
        for (User u : List.of(student1, student2)) {
            usageRequestRepository.findByStudentOrderByCreatedAtDesc(u).stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED || r.getStatus() == RequestStatus.CHECKED_IN)
                    .forEach(r -> {
                        r.setStatus(RequestStatus.CANCELLED);
                        usageRequestRepository.save(r);
                    });
        }

        elibrary = facilityRepository.findAll().stream()
                .filter(f -> "E-Library".equalsIgnoreCase(f.getName()))
                .findFirst()
                .orElseThrow();

        eLibraryService.initializeELibraryWorkstations();

        org.springframework.security.core.Authentication auth1 =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        student1.getEmail(), null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STUDENT")));
        org.springframework.security.core.Authentication auth2 =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        student2.getEmail(), null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STUDENT")));

        student1Token = "Bearer " + jwtTokenProvider.generateToken(auth1);
        student2Token = "Bearer " + jwtTokenProvider.generateToken(auth2);
    }

    @Test
    @DisplayName("GET /api/elibrary/floorplan returns all 68 workstations with live status and legend totals")
    void testGetFloorPlanEndpoint() throws Exception {
        mockMvc.perform(get("/api/elibrary/floorplan")
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facilityName", is("E-Library")))
                .andExpect(jsonPath("$.totalSeats", is(68)))
                .andExpect(jsonPath("$.seats", hasSize(68)))
                .andExpect(jsonPath("$.seats[0].seatNumber", is(1)))
                .andExpect(jsonPath("$.seats[6].seatNumber", is(7)))
                .andExpect(jsonPath("$.seats[6].status", is("MAINTENANCE")))
                .andExpect(jsonPath("$.seats[50].seatNumber", is(51)))
                .andExpect(jsonPath("$.seats[67].seatNumber", is(68)));
    }

    @Test
    @DisplayName("GET /api/elibrary/seats/51 returns workstation 51 details")
    void testGetSeatByNumberEndpoint() throws Exception {
        mockMvc.perform(get("/api/elibrary/seats/51")
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatNumber", is(51)))
                .andExpect(jsonPath("$.type", is("PC")))
                .andExpect(jsonPath("$.bookable", is(true)));
    }

    @Test
    @DisplayName("POST /api/requests creates pending booking and prevents concurrent duplicate request for same seat")
    void testCreateBookingAndPreventConcurrentConflict() throws Exception {
        Resource workstation20 = resourceRepository.findAll().stream()
                .filter(r -> r.getFacility().getId().equals(elibrary.getId()) && ELibraryService.extractSeatNumber(r.getName()) == 20)
                .findFirst()
                .orElseThrow();

        workstation20.setStatus(ResourceStatus.AVAILABLE);
        resourceRepository.save(workstation20);

        CreateUsageRequestDto req1 = new CreateUsageRequestDto();
        req1.setResourceId(workstation20.getId());
        req1.setExpectedArrival("Immediate (Next 10 mins)");
        req1.setExpectedDuration("1 hour");
        req1.setGroupSize(1);

        // 1. Student 1 requests Workstation 20 -> 201 Created
        mockMvc.perform(post("/api/requests")
                        .header("Authorization", student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.requestCode", notNullValue()));

        // 2. Student 2 tries to request the same workstation 20 -> 400 Bad Request
        CreateUsageRequestDto req2 = new CreateUsageRequestDto();
        req2.setResourceId(workstation20.getId());
        req2.setExpectedArrival("Immediate (Next 10 mins)");
        req2.setExpectedDuration("1 hour");
        req2.setGroupSize(1);

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", student2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unavailable")));
    }
}
