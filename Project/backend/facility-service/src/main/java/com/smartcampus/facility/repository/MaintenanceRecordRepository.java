package com.smartcampus.facility.repository;

import com.smartcampus.facility.entity.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long>, JpaSpecificationExecutor<MaintenanceRecord> {

    Page<MaintenanceRecord> findByFacilityId(Long facilityId, Pageable pageable);

    Optional<MaintenanceRecord> findByIdAndFacilityId(Long id, Long facilityId);

    long countByFacilityId(Long facilityId);
}
