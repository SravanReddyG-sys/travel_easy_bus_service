package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
    List<Route> findByOperatorId(UUID operatorId);
    Page<Route> findByOperatorId(UUID operatorId, Pageable pageable);
    Optional<Route> findByIdAndOperatorId(UUID id, UUID operatorId);
    boolean existsByOperatorIdAndRouteCode(UUID operatorId, String routeCode);

    @Query("SELECT r FROM Route r WHERE LOWER(r.origin) = LOWER(:origin) AND LOWER(r.destination) = LOWER(:destination) AND r.isActive = true")
    List<Route> findActiveRoutesByOriginAndDestination(@Param("origin") String origin, @Param("destination") String destination);
}
