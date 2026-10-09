package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.BoardingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardingPointRepository extends JpaRepository<BoardingPoint, UUID> {
    List<BoardingPoint> findByRouteIdOrderByDepartureOffsetMinutesAsc(UUID routeId);
    void deleteByRouteId(UUID routeId);
}
