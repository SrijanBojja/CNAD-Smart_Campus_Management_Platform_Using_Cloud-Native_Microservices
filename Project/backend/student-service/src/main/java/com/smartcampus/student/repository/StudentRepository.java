package com.smartcampus.student.repository;

import com.smartcampus.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByStudentNumber(String studentNumber);
    boolean existsByUserId(Long userId);
    boolean existsByStudentNumber(String studentNumber);
    Page<Student> findAll(Pageable pageable);
}
