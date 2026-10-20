package com.queuesense.dto;

import com.queuesense.entity.ResourceItem;
import java.time.LocalDateTime;

public class ResourceItemDto {
    private Long id;
    private Long resourceId;
    private Integer unitNumber;
    private String itemCode;
    private String name;
    private String status;
    private String conditionNotes;
    private LocalDateTime updatedAt;

    public ResourceItemDto() {}

    public static ResourceItemDto fromEntity(ResourceItem item) {
        ResourceItemDto dto = new ResourceItemDto();
        dto.setId(item.getId());
        if (item.getResource() != null) {
            dto.setResourceId(item.getResource().getId());
        }
        dto.setUnitNumber(item.getUnitNumber());
        dto.setItemCode(item.getItemCode());
        dto.setName(item.getName());
        dto.setStatus(item.getStatus());
        dto.setConditionNotes(item.getConditionNotes());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Integer getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
