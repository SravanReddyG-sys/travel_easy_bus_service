package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.Bus;
import com.traveleasy.bus.enums.BusStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusRepository extends JpaRepository<Bus, UUID> {
    List<Bus> findByOperatorId(UUID operatorId);
    Page<Bus> findByOperatorId(UUID operatorId, Pageable pageable);
    Optional<Bus> findByIdAndOperatorId(UUID id, UUID operatorId);
    boolean existsByRegistrationNumber(String registrationNumber);
    boolean existsByOperatorIdAndBusNumber(UUID operatorId, String busNumber);
    List<Bus> findByOperatorIdAndStatus(UUID operatorId, BusStatus status);
}
