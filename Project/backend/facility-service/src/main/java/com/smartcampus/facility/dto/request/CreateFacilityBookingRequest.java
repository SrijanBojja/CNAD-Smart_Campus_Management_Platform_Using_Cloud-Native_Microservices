package com.smartcampus.facility.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Request body to book/request a facility")
public class CreateFacilityBookingRequest {

    @NotNull(message = "Request date is required")
    @FutureOrPresent(message = "Request date cannot be in the past")
    @Schema(description = "Date of the facility booking", example = "2026-10-15")
    private LocalDate requestDate;

    @NotNull(message = "Start time is required")
    @Schema(description = "Booking start time (HH:mm:ss)", example = "09:00:00")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @Schema(description = "Booking end time (HH:mm:ss)", example = "11:00:00")
    private LocalTime endTime;

    @NotBlank(message = "Purpose is required")
    @Size(max = 300, message = "Purpose must not exceed 300 characters")
    @Schema(description = "Purpose of booking", example = "Annual Computer Science Symposium Keynote")
    private String purpose;

    public CreateFacilityBookingRequest() {
    }

    public CreateFacilityBookingRequest(LocalDate requestDate, LocalTime startTime, LocalTime endTime, String purpose) {
        this.requestDate = requestDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
