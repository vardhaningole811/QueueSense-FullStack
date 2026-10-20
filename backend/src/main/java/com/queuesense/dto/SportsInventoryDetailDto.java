package com.queuesense.dto;

import java.util.ArrayList;
import java.util.List;

public class SportsInventoryDetailDto {
    private SportsResourceDto resource;
    private List<ResourceItemDto> items = new ArrayList<>();

    public SportsInventoryDetailDto() {}

    public SportsInventoryDetailDto(SportsResourceDto resource, List<ResourceItemDto> items) {
        this.resource = resource;
        this.items = items;
    }

    public SportsResourceDto getResource() { return resource; }
    public void setResource(SportsResourceDto resource) { this.resource = resource; }

    public List<ResourceItemDto> getItems() { return items; }
    public void setItems(List<ResourceItemDto> items) { this.items = items; }
}
