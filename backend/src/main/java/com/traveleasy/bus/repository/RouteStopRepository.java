package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {
    List<RouteStop> findByRouteIdOrderByStopOrderAsc(UUID routeId);
    Optional<RouteStop> findByRouteIdAndStopOrder(UUID routeId, Integer stopOrder);
    boolean existsByRouteIdAndStopOrder(UUID routeId, Integer stopOrder);
    void deleteByRouteId(UUID routeId);
}
