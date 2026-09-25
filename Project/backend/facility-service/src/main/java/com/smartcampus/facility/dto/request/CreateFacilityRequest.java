package com.smartcampus.facility.dto.request;

import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to create a new facility")
public class CreateFacilityRequest {

    @NotBlank(message = "Facility name is required")
    @Size(max = 150, message = "Facility name must not exceed 150 characters")
    @Schema(description = "Name of the facility", example = "Auditorium Main Hall")
    private String name;

    @NotNull(message = "Facility type is required")
    @Schema(description = "Type of facility", example = "AUDITORIUM")
    private FacilityType facilityType;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Description and equipment details", example = "Central auditorium with 500 seats and A/V equipment")
    private String description;

    @Size(max = 150, message = "Building name must not exceed 150 characters")
    @Schema(description = "Building name", example = "Main Academic Block")
    private String building;

    @Size(max = 50, message = "Room number must not exceed 50 characters")
    @Schema(description = "Room number or designation", example = "AH-101")
    private String roomNumber;

    @Positive(message = "Capacity must be a positive integer")
    @Schema(description = "Seating / occupant capacity", example = "500")
    private Integer capacity;

    @Schema(description = "Facility status", example = "AVAILABLE")
    private FacilityStatus status = FacilityStatus.AVAILABLE;

    public CreateFacilityRequest() {
    }

    public CreateFacilityRequest(String name, FacilityType facilityType, String description,
                                 String building, String roomNumber, Integer capacity, FacilityStatus status) {
        this.name = name;
        this.facilityType = facilityType;
        this.description = description;
        this.building = building;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.status = status != null ? status : FacilityStatus.AVAILABLE;
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
}
