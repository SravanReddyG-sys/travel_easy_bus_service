package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.BusEventOutbox;
import com.traveleasy.bus.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusEventOutboxRepository extends JpaRepository<BusEventOutbox, UUID> {
    List<BusEventOutbox> findByStatusOrderByCreatedAtAsc(EventStatus status, Pageable pageable);
    Optional<BusEventOutbox> findByEventId(UUID eventId);
    List<BusEventOutbox> findByAggregateId(UUID aggregateId);
}
