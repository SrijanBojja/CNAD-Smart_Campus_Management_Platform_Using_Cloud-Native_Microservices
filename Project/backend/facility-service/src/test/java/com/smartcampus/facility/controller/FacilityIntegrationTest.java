package com.smartcampus.facility.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.facility.dto.request.*;
import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.FacilityRequestStatus;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import com.smartcampus.facility.entity.MaintenancePriority;
import com.smartcampus.facility.entity.MaintenanceStatus;
import com.smartcampus.facility.repository.FacilityRepository;
import com.smartcampus.facility.repository.FacilityRequestRepository;
import com.smartcampus.facility.repository.MaintenanceRecordRepository;
import com.smartcampus.facility.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FacilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private FacilityRequestRepository facilityRequestRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String adminToken;
    private String facultyToken;
    private String student1Token;
    private String student2Token;

    @BeforeEach
    void cleanAndSetup() {
        maintenanceRecordRepository.deleteAll();
        facilityRequestRepository.deleteAll();
        facilityRepository.deleteAll();

        adminToken = "Bearer " + jwtTokenProvider.generateToken(1L, "adminUser", "admin@smartcampus.edu", List.of("ADMIN"));
        facultyToken = "Bearer " + jwtTokenProvider.generateToken(2L, "facultyUser", "faculty@smartcampus.edu", List.of("FACULTY"));
        student1Token = "Bearer " + jwtTokenProvider.generateToken(101L, "student1", "student1@smartcampus.edu", List.of("STUDENT"));
        student2Token = "Bearer " + jwtTokenProvider.generateToken(102L, "student2", "student2@smartcampus.edu", List.of("STUDENT"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/facilities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Complete Facility, Booking, and Maintenance Lifecycle")
    void testCompleteLifecycle() throws Exception {
        // 1. Create Facility as ADMIN
        CreateFacilityRequest createFacility = new CreateFacilityRequest(
                "Main Auditorium",
                FacilityType.AUDITORIUM,
                "500 capacity auditorium with projector and sound",
                "Central Block",
                "AUD-101",
                500,
                FacilityStatus.AVAILABLE
        );

        String facilityJson = mockMvc.perform(post("/api/v1/facilities")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createFacility)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Main Auditorium"))
                .andExpect(jsonPath("$.facilityType").value("AUDITORIUM"))
                .andReturn().getResponse().getContentAsString();

        Long facilityId = objectMapper.readTree(facilityJson).get("id").asLong();

        // 2. Student 1 creates booking request for day after tomorrow 10:00 to 12:00
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        CreateFacilityBookingRequest createBooking = new CreateFacilityBookingRequest(
                bookingDate,
                LocalTime.of(10, 0, 0),
                LocalTime.of(12, 0, 0),
                "Robotics Club Annual Showcase"
        );

        String bookingJson = mockMvc.perform(post("/api/v1/facilities/" + facilityId + "/requests")
                        .header("Authorization", student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.requestedByUserId").value(101L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Long requestId = objectMapper.readTree(bookingJson).get("id").asLong();

        // 3. Student 1 views own request -> 200 OK
        mockMvc.perform(get("/api/v1/facilities/" + facilityId + "/requests/" + requestId)
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.purpose").value("Robotics Club Annual Showcase"));

        // 4. Student 2 views Student 1's request -> 403 Forbidden
        mockMvc.perform(get("/api/v1/facilities/" + facilityId + "/requests/" + requestId)
                        .header("Authorization", student2Token))
                .andExpect(status().isForbidden());

        // 5. Student 1 attempts to approve request -> 403 Forbidden
        UpdateFacilityRequestStatusRequest approveRequest = new UpdateFacilityRequestStatusRequest(
                FacilityRequestStatus.APPROVED, "Self approved"
        );
        mockMvc.perform(patch("/api/v1/facilities/" + facilityId + "/requests/" + requestId + "/status")
                        .header("Authorization", student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isForbidden());

        // 6. Faculty approves request -> 200 OK (approvedByUserId set to 2L)
        UpdateFacilityRequestStatusRequest facultyApprove = new UpdateFacilityRequestStatusRequest(
                FacilityRequestStatus.APPROVED, "Approved by Department Chair"
        );
        mockMvc.perform(patch("/api/v1/facilities/" + facilityId + "/requests/" + requestId + "/status")
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facultyApprove)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedByUserId").value(2L))
                .andExpect(jsonPath("$.remarks").value("Approved by Department Chair"));

        // 7. Student 2 creates conflicting booking overlapping with approved booking (11:00 - 13:00) -> 409 Conflict
        CreateFacilityBookingRequest conflictingBooking = new CreateFacilityBookingRequest(
                bookingDate,
                LocalTime.of(11, 0, 0),
                LocalTime.of(13, 0, 0),
                "Overlapping meeting"
        );
        mockMvc.perform(post("/api/v1/facilities/" + facilityId + "/requests")
                        .header("Authorization", student2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingBooking)))
                .andExpect(status().isConflict());

        // 8. Faculty reports maintenance issue
        CreateMaintenanceRecordRequest maintenanceReq = new CreateMaintenanceRecordRequest(
                "Air conditioning unit not cooling effectively",
                MaintenancePriority.HIGH,
                15L
        );
        String maintJson = mockMvc.perform(post("/api/v1/facilities/" + facilityId + "/maintenance")
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintenanceReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.reportedByUserId").value(2L))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        Long maintenanceId = objectMapper.readTree(maintJson).get("id").asLong();

        // 9. Admin updates maintenance status to RESOLVED
        UpdateMaintenanceStatusRequest resolveMaint = new UpdateMaintenanceStatusRequest(
                MaintenanceStatus.RESOLVED,
                15L,
                "AC coolant refilled and thermostat recalibrated"
        );
        mockMvc.perform(patch("/api/v1/facilities/" + facilityId + "/maintenance/" + maintenanceId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveMaint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        // 10. Attempt safe deletion of facility when history exists -> status becomes UNAVAILABLE, not physically removed
        mockMvc.perform(delete("/api/v1/facilities/" + facilityId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Verify facility still exists and has UNAVAILABLE status
        mockMvc.perform(get("/api/v1/facilities/" + facilityId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
    }
}
