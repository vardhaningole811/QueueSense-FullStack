package com.queuesense.config;

import com.queuesense.entity.*;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.enums.Role;
import com.queuesense.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OperatorProfileRepository operatorProfileRepository;
    private final FacilityRepository facilityRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.queuesense.service.SportsInventoryService sportsInventoryService;
    private final com.queuesense.service.ELibraryService eLibraryService;

    public DataInitializer(UserRepository userRepository,
                           StudentProfileRepository studentProfileRepository,
                           OperatorProfileRepository operatorProfileRepository,
                           FacilityRepository facilityRepository,
                           ResourceRepository resourceRepository,
                           PasswordEncoder passwordEncoder,
                           com.queuesense.service.SportsInventoryService sportsInventoryService,
                           com.queuesense.service.ELibraryService eLibraryService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.operatorProfileRepository = operatorProfileRepository;
        this.facilityRepository = facilityRepository;
        this.resourceRepository = resourceRepository;
        this.passwordEncoder = passwordEncoder;
        this.sportsInventoryService = sportsInventoryService;
        this.eLibraryService = eLibraryService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Always ensure sports inventory upgrade is initialized
        sportsInventoryService.initializeSportsInventory();
        // Always ensure full 68-workstation E-Library seating arrangement is initialized
        eLibraryService.initializeELibraryWorkstations();

        if (userRepository.count() > 0) {
            return; // Already seeded
        }

        // 1. Seed Facilities
        Facility elibrary = new Facility("E-Library", "LIBRARY", "Central digital library with high-speed workstations and quiet study pods");
        Facility readingRoom = new Facility("Reading Room", "LIBRARY", "Quiet general study area with individual desks and reference books");
        Facility sportsComplex = new Facility("Sports Complex", "SPORTS", "Campus athletic complex featuring indoor and outdoor courts");

        elibrary = facilityRepository.save(elibrary);
        readingRoom = facilityRepository.save(readingRoom);
        sportsComplex = facilityRepository.save(sportsComplex);

        // 2. Seed Users
        // Student
        User studentUser = new User("student@queuesense.demo", passwordEncoder.encode("Student@123"), Role.ROLE_STUDENT, "Aarav Sharma");
        studentUser = userRepository.save(studentUser);
        StudentProfile studentProfile = new StudentProfile(studentUser, "STU-2026-001", "Computer Science & Engineering", 3);
        studentProfileRepository.save(studentProfile);

        // Operator
        User operatorUser = new User("operator@queuesense.demo", passwordEncoder.encode("Operator@123"), Role.ROLE_OPERATOR, "Dr. Rajesh Kulkarni");
        operatorUser = userRepository.save(operatorUser);
        OperatorProfile operatorProfile = new OperatorProfile(operatorUser, "EMP-2026-088", elibrary);
        operatorProfileRepository.save(operatorProfile);

        // Admin
        User adminUser = new User("admin@queuesense.demo", passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN, "Campus Admin");
        userRepository.save(adminUser);

        // 3. Seed Resources for E-Library
        resourceRepository.save(new Resource(elibrary, "Seat 01", "SEAT", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(elibrary, "Seat 02", "SEAT", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(elibrary, "Seat 03", "SEAT", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(elibrary, "PC 04", "PC", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(elibrary, "Seat 05", "SEAT", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(elibrary, "Seat 06", "SEAT", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(elibrary, "Seat 07", "SEAT", ResourceStatus.MAINTENANCE));
        resourceRepository.save(new Resource(elibrary, "PC 08", "PC", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(elibrary, "Seat 09", "SEAT", ResourceStatus.AVAILABLE));
        for (int i = 10; i <= 68; i++) {
            resourceRepository.save(new Resource(elibrary, String.format("Workstation %02d", i), "PC", ResourceStatus.AVAILABLE));
        }

        // Seed Resources for Reading Room
        resourceRepository.save(new Resource(readingRoom, "Desk A-1", "DESK", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(readingRoom, "Desk A-2", "DESK", ResourceStatus.AVAILABLE));
        resourceRepository.save(new Resource(readingRoom, "Desk B-1", "DESK", ResourceStatus.AVAILABLE));

        // Seed Resources for Sports Complex via SportsInventoryService
        sportsInventoryService.initializeSportsInventory();

        System.out.println(">>> QueueSense Demo Seed Data Initialized Successfully! <<<");
    }
}
