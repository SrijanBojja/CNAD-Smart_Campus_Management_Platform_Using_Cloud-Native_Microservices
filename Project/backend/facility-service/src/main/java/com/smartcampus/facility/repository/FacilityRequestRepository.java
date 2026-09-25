package com.smartcampus.facility.repository;

import com.smartcampus.facility.entity.FacilityRequest;
import com.smartcampus.facility.entity.FacilityRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface FacilityRequestRepository extends JpaRepository<FacilityRequest, Long>, JpaSpecificationExecutor<FacilityRequest> {

    Page<FacilityRequest> findByFacilityId(Long facilityId, Pageable pageable);

    Optional<FacilityRequest> findByIdAndFacilityId(Long id, Long facilityId);

    long countByFacilityId(Long facilityId);

    @Query("SELECT COUNT(r) > 0 FROM FacilityRequest r " +
           "WHERE r.facility.id = :facilityId " +
           "AND r.requestDate = :requestDate " +
           "AND r.status = :status " +
           "AND (:excludeRequestId IS NULL OR r.id != :excludeRequestId) " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsConflictingApprovedRequest(@Param("facilityId") Long facilityId,
                                            @Param("requestDate") LocalDate requestDate,
                                            @Param("status") FacilityRequestStatus status,
                                            @Param("startTime") LocalTime startTime,
                                            @Param("endTime") LocalTime endTime,
                                            @Param("excludeRequestId") Long excludeRequestId);
}
