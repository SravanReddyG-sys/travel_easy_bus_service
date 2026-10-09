package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.BusOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusOperatorRepository extends JpaRepository<BusOperator, UUID> {
    Optional<BusOperator> findByOperatorCode(String operatorCode);
    boolean existsByOperatorCode(String operatorCode);
}
