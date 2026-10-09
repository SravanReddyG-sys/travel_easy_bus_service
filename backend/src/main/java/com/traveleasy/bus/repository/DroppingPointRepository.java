package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.DroppingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DroppingPointRepository extends JpaRepository<DroppingPoint, UUID> {
    List<DroppingPoint> findByRouteIdOrderByArrivalOffsetMinutesAsc(UUID routeId);
    void deleteByRouteId(UUID routeId);
}
