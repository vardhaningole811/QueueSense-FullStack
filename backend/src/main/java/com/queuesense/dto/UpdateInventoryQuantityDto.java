package com.queuesense.dto;

import jakarta.validation.constraints.Min;

public class UpdateInventoryQuantityDto {
    @Min(value = 0, message = "Total quantity cannot be negative")
    private int newTotalQuantity;

    public UpdateInventoryQuantityDto() {}

    public UpdateInventoryQuantityDto(int newTotalQuantity) {
        this.newTotalQuantity = newTotalQuantity;
    }

    public int getNewTotalQuantity() { return newTotalQuantity; }
    public void setNewTotalQuantity(int newTotalQuantity) { this.newTotalQuantity = newTotalQuantity; }
}
