package com.queuesense.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuesense.dto.analytics.CrowdAnalyticsResponseDto;
import com.queuesense.dto.analytics.CrowdSummaryDto;
import com.queuesense.dto.analytics.ForecastHorizonDto;
import com.queuesense.dto.analytics.ModelTransparencyDto;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.FacilityRepository;
import com.queuesense.repository.ResourceRepository;
import com.queuesense.repository.UsageSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CrowdAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CrowdAnalyticsService.class);

    private final FacilityRepository facilityRepository;
    private final ResourceRepository resourceRepository;
    private final UsageSessionRepository usageSessionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ml.service-url:http://localhost:5001}")
    private String mlServiceUrl;

    public CrowdAnalyticsService(FacilityRepository facilityRepository,
                                 ResourceRepository resourceRepository,
                                 UsageSessionRepository usageSessionRepository,
                                 RestTemplateBuilder restTemplateBuilder,
                                 ObjectMapper objectMapper) {
        this.facilityRepository = facilityRepository;
        this.resourceRepository = resourceRepository;
        this.usageSessionRepository = usageSessionRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(2000))
                .setReadTimeout(Duration.ofMillis(3000))
                .build();
        this.objectMapper = objectMapper;
    }

    public List<CrowdSummaryDto> getAllFacilitiesCrowdSummary() {
        List<Facility> facilities = facilityRepository.findByActiveTrue();
        List<CrowdSummaryDto> summaries = new ArrayList<>();

        for (Facility facility : facilities) {
            List<Resource> resources = resourceRepository.findByFacilityIdAndActiveTrue(facility.getId());
            long usableCapacity = resources.stream()
                    .filter(r -> r.getStatus() != ResourceStatus.MAINTENANCE)
                    .count();

            long activeSessions = usageSessionRepository.countByResourceFacilityIdAndStatus(facility.getId(), "ACTIVE");
            Double occupancyPct = usableCapacity > 0 ? (activeSessions * 100.0) / usableCapacity : null;
            String crowdLevel = deriveCrowdLevel(occupancyPct);
            long available = Math.max(0, usableCapacity - activeSessions);

            summaries.add(new CrowdSummaryDto(
                    facility.getId(),
                    facility.getName(),
                    facility.getType(),
                    activeSessions,
                    usableCapacity,
                    occupancyPct != null ? Math.round(occupancyPct * 10.0) / 10.0 : null,
                    crowdLevel,
                    available
            ));
        }

        return summaries;
    }

    public CrowdAnalyticsResponseDto getFacilityCrowdAnalytics(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + facilityId));

        List<Resource> resources = resourceRepository.findByFacilityIdAndActiveTrue(facility.getId());
        long totalResources = resources.size();
        long maintenanceCount = resources.stream()
                .filter(r -> r.getStatus() == ResourceStatus.MAINTENANCE)
                .count();
        long usableCapacity = totalResources - maintenanceCount;

        long activeSessions = usageSessionRepository.countByResourceFacilityIdAndStatus(facility.getId(), "ACTIVE");
        Double occupancyPct = usableCapacity > 0 ? (activeSessions * 100.0) / usableCapacity : null;
        String recordedCrowdLevel = deriveCrowdLevel(occupancyPct);
        long available = Math.max(0, usableCapacity - activeSessions);

        CrowdAnalyticsResponseDto dto = new CrowdAnalyticsResponseDto();
        dto.setFacilityId(facility.getId());
        dto.setFacilityName(facility.getName());
        dto.setFacilityType(facility.getType());
        dto.setRecordedActiveSessions(activeSessions);
        dto.setUsableCapacity(usableCapacity);
        dto.setTotalResources(totalResources);
        dto.setMaintenanceResources(maintenanceCount);
        dto.setRecordedOccupancyPercentage(occupancyPct != null ? Math.round(occupancyPct * 10.0) / 10.0 : null);
        dto.setRecordedCrowdLevel(recordedCrowdLevel);
        dto.setAvailableUsableResources(available);
        dto.setAsOfTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setOccupancyTypeClarification(
                "Recorded occupancy reflects active checked-in usage sessions. Physical occupancy may differ if patrons are present without digital check-in. Inactive and maintenance resources are excluded from capacity."
        );

        // Attempt ML service forecast
        enrichWithMlForecast(dto, facility, activeSessions, usableCapacity);

        return dto;
    }

    private void enrichWithMlForecast(CrowdAnalyticsResponseDto dto, Facility facility, long activeSessions, long usableCapacity) {
        try {
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("facility_id", facility.getId());
            requestPayload.put("facility_name", facility.getName());
            requestPayload.put("current_recorded_sessions", activeSessions);
            requestPayload.put("usable_capacity", usableCapacity);
            requestPayload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    mlServiceUrl + "/predict",
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                dto.setForecastStatus(root.path("status").asText("AVAILABLE"));

                List<ForecastHorizonDto> forecasts = new ArrayList<>();
                JsonNode forecastsNode = root.path("forecasts");
                if (forecastsNode.isArray()) {
                    for (JsonNode fNode : forecastsNode) {
                        int horizon = fNode.path("horizon_minutes").asInt();
                        String targetTime = fNode.path("target_timestamp").asText();
                        double predictedOccupancy = fNode.path("predicted_occupancy").asDouble();
                        Double predictedPct = usableCapacity > 0 ? (predictedOccupancy * 100.0) / usableCapacity : null;
                        String crowdLevel = deriveCrowdLevel(predictedPct);
                        String confidenceNote = fNode.path("confidence_note").asText();

                        forecasts.add(new ForecastHorizonDto(
                                horizon,
                                targetTime,
                                Math.round(predictedOccupancy * 10.0) / 10.0,
                                predictedPct != null ? Math.round(predictedPct * 10.0) / 10.0 : null,
                                crowdLevel,
                                confidenceNote
                        ));
                    }
                }
                dto.setForecasts(forecasts);

                // Estimated time until full
                if (root.hasNonNull("estimated_time_until_full_minutes")) {
                    dto.setEstimatedTimeUntilFullMinutes(root.get("estimated_time_until_full_minutes").asInt());
                }
                dto.setTimeUntilFullMessage(root.path("time_until_full_message").asText("Not enough data to estimate"));
                dto.setBestTimeToVisit(root.path("best_time_to_visit").asText("Standard library operating hours"));

                // Model transparency
                JsonNode transNode = root.path("transparency");
                ModelTransparencyDto transparency = new ModelTransparencyDto();
                transparency.setPrototype(transNode.path("is_prototype").asBoolean(true));
                transparency.setDataSource(transNode.path("data_source").asText("SYNTHETIC PROTOTYPE DATASET"));
                transparency.setModelSelected(transNode.path("model_selected").asText("RandomForestRegressor"));
                if (transNode.hasNonNull("validation_mae")) transparency.setValidationMae(transNode.get("validation_mae").asDouble());
                if (transNode.hasNonNull("validation_rmse")) transparency.setValidationRmse(transNode.get("validation_rmse").asDouble());
                if (transNode.hasNonNull("baseline_mae")) transparency.setBaselineMae(transNode.get("baseline_mae").asDouble());
                if (transNode.hasNonNull("baseline_rmse")) transparency.setBaselineRmse(transNode.get("baseline_rmse").asDouble());
                transparency.setHorizons(List.of(15, 30, 60));
                transparency.setDisclaimer(transNode.path("disclaimer").asText("PROTOTYPE — REAL-WORLD VALIDATION PENDING."));
                transparency.setTrainingTimestamp(transNode.path("training_timestamp").asText());
                if (transNode.hasNonNull("sample_size")) transparency.setSampleSize(transNode.get("sample_size").asInt());

                dto.setModelTransparency(transparency);
                return;
            }
        } catch (RestClientException e) {
            log.warn("Python ML inference microservice unreachable at {}: {}", mlServiceUrl, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to parse ML service response: {}", e.getMessage());
        }

        // Graceful offline fallback: Provide live metrics + explicit offline status without pretending fake forecasts exist
        dto.setForecastStatus("ML_SERVICE_OFFLINE");
        dto.setForecasts(Collections.emptyList());
        dto.setEstimatedTimeUntilFullMinutes(null);
        dto.setTimeUntilFullMessage("Not enough data to estimate (ML service offline)");
        dto.setBestTimeToVisit("Real-time physical occupancy forecasting offline. Start ML inference service ('python ml/service.py') to view predicted low-crowd windows.");

        ModelTransparencyDto fallbackTrans = new ModelTransparencyDto();
        fallbackTrans.setPrototype(true);
        fallbackTrans.setDataSource("SYNTHETIC PROTOTYPE PIPELINE — Service Offline");
        fallbackTrans.setModelSelected("None (Service Offline)");
        fallbackTrans.setHorizons(List.of(15, 30, 60));
        fallbackTrans.setDisclaimer(
                "PROTOTYPE — REAL-WORLD VALIDATION PENDING. The Python ML inference microservice is currently offline. Real-time recorded occupancy is displayed based on digital session check-ins."
        );
        dto.setModelTransparency(fallbackTrans);
    }

    private String deriveCrowdLevel(Double occupancyPercentage) {
        if (occupancyPercentage == null) return "UNKNOWN";
        if (occupancyPercentage < 40.0) return "LOW";
        if (occupancyPercentage < 75.0) return "MODERATE";
        if (occupancyPercentage < 90.0) return "HIGH";
        return "FULL";
    }
}
