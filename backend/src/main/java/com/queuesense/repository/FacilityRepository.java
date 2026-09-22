package com.queuesense.repository;

import com.queuesense.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByActiveTrue();
    Optional<Facility> findByName(String name);
}
