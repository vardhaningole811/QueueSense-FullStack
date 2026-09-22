package com.queuesense.service;

import com.queuesense.dto.DashboardStatsDto;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.repository.CheckInRepository;
import com.queuesense.repository.ResourceRepository;
import com.queuesense.repository.UsageRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final UsageRequestRepository usageRequestRepository;
    private final ResourceRepository resourceRepository;
    private final CheckInRepository checkInRepository;

    public DashboardService(UsageRequestRepository usageRequestRepository,
                            ResourceRepository resourceRepository,
                            CheckInRepository checkInRepository) {
        this.usageRequestRepository = usageRequestRepository;
        this.resourceRepository = resourceRepository;
        this.checkInRepository = checkInRepository;
    }

    public DashboardStatsDto getDashboardStats() {
        long pending = usageRequestRepository.countByStatus(RequestStatus.PENDING);
        long available = resourceRepository.countByStatus(ResourceStatus.AVAILABLE);
        long reserved = resourceRepository.countByStatus(ResourceStatus.RESERVED);
        long occupied = resourceRepository.countByStatus(ResourceStatus.OCCUPIED);
        long maintenance = resourceRepository.countByStatus(ResourceStatus.MAINTENANCE);
        long total = resourceRepository.count();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayCheckIns = checkInRepository.countTodayCheckIns(startOfDay);

        return new DashboardStatsDto(pending, available, reserved, occupied, maintenance, total, todayCheckIns);
    }
}
