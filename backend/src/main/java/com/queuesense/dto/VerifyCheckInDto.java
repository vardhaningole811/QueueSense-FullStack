package com.queuesense.dto;

import com.queuesense.enums.CheckInMethod;
import jakarta.validation.constraints.NotBlank;

public class VerifyCheckInDto {
    @NotBlank(message = "Request code is required")
    private String requestCode;

    private CheckInMethod method = CheckInMethod.QR_SCAN;

    public VerifyCheckInDto() {}

    public VerifyCheckInDto(String requestCode, CheckInMethod method) {
        this.requestCode = requestCode;
        this.method = method;
    }

    public String getRequestCode() { return requestCode; }
    public void setRequestCode(String requestCode) { this.requestCode = requestCode; }

    public CheckInMethod getMethod() { return method; }
    public void setMethod(CheckInMethod method) { this.method = method; }
}
