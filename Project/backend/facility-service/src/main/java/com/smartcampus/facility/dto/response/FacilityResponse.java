package com.smartcampus.facility.dto.response;

import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response representing facility details")
public class FacilityResponse {

    @Schema(description = "Facility ID", example = "1")
    private Long id;

    @Schema(description = "Facility name", example = "Auditorium Main Hall")
    private String name;

    @Schema(description = "Facility type", example = "AUDITORIUM")
    private FacilityType facilityType;

    @Schema(description = "Facility description", example = "Central auditorium with 500 seats and A/V equipment")
    private String description;

    @Schema(description = "Building name", example = "Main Academic Block")
    private String building;

    @Schema(description = "Room number or designation", example = "AH-101")
    private String roomNumber;

    @Schema(description = "Capacity", example = "500")
    private Integer capacity;

    @Schema(description = "Status", example = "AVAILABLE")
    private FacilityStatus status;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public FacilityResponse() {
    }

    public static FacilityResponse fromEntity(Facility facility) {
        if (facility == null) {
            return null;
        }
        FacilityResponse response = new FacilityResponse();
        response.setId(facility.getId());
        response.setName(facility.getName());
        response.setFacilityType(facility.getFacilityType());
        response.setDescription(facility.getDescription());
        response.setBuilding(facility.getBuilding());
        response.setRoomNumber(facility.getRoomNumber());
        response.setCapacity(facility.getCapacity());
        response.setStatus(facility.getStatus());
        response.setCreatedAt(facility.getCreatedAt());
        response.setUpdatedAt(facility.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public FacilityStatus getStatus() {
        return status;
    }

    public void setStatus(FacilityStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
