package com.traveleasy.bus.service.impl;

import com.traveleasy.bus.dto.request.BusSearchRequest;
import com.traveleasy.bus.dto.response.*;
import com.traveleasy.bus.entity.Fare;
import com.traveleasy.bus.entity.Route;
import com.traveleasy.bus.entity.Schedule;
import com.traveleasy.bus.enums.BusType;
import com.traveleasy.bus.mapper.RouteMapper;
import com.traveleasy.bus.repository.FareRepository;
import com.traveleasy.bus.repository.ScheduleRepository;
import com.traveleasy.bus.repository.ScheduleSeatInventoryRepository;
import com.traveleasy.bus.service.BusSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusSearchServiceImpl implements BusSearchService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatInventoryRepository inventoryRepository;
    private final FareRepository fareRepository;
    private final RouteMapper routeMapper;

    @Override
    @Transactional(readOnly = true)
    public BusSearchPageResponse searchBuses(BusSearchRequest request) {
        log.info("Searching buses from [{}] to [{}] on date [{}] with sortBy [{}]",
                request.getOrigin(), request.getDestination(), request.getTravelDate(), request.getSortBy());

        String origin = request.getOrigin().trim();
        String destination = request.getDestination().trim();

        // 1. Fetch matching active schedules
        List<Schedule> matchingSchedules = scheduleRepository.findAvailableSchedulesForSearch(
                origin, destination, request.getTravelDate());

        if (matchingSchedules.isEmpty()) {
            return BusSearchPageResponse.builder()
                    .content(Collections.emptyList())
                    .page(request.getPage())
                    .size(request.getSize())
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .filterSummary(BusSearchFilterSummaryResponse.builder()
                            .totalFound(0)
                            .minPrice(BigDecimal.ZERO)
                            .maxPrice(BigDecimal.ZERO)
                            .build())
                    .build();
        }

        // 2. Map and enrich all matching cards
        List<BusSearchResultCardResponse> allCards = new ArrayList<>();
        Set<BusType> facetBusTypes = new HashSet<>();
        Set<String> facetOperators = new HashSet<>();
        Set<String> facetBoardingPoints = new HashSet<>();
        Set<String> facetDroppingPoints = new HashSet<>();
        BigDecimal overallMinPrice = null;
        BigDecimal overallMaxPrice = null;

        for (Schedule schedule : matchingSchedules) {
            // Calculate starting fare
            BigDecimal startingFare = calculateStartingFare(schedule);
            if (overallMinPrice == null || startingFare.compareTo(overallMinPrice) < 0) {
                overallMinPrice = startingFare;
            }
            if (overallMaxPrice == null || startingFare.compareTo(overallMaxPrice) > 0) {
                overallMaxPrice = startingFare;
            }

            // Facet tracking
            if (schedule.getBus().getBusType() != null) {
                facetBusTypes.add(schedule.getBus().getBusType());
            }
            if (schedule.getOperator().getCompanyName() != null) {
                facetOperators.add(schedule.getOperator().getCompanyName());
            }

            // Route points mapping
            Route route = schedule.getRoute();
            List<BoardingPointResponse> boardingResponses = route.getBoardingPoints() != null ?
                    route.getBoardingPoints().stream()
                            .map(routeMapper::toBoardingResponse)
                            .collect(Collectors.toList()) :
                    Collections.emptyList();

            List<DroppingPointResponse> droppingResponses = route.getDroppingPoints() != null ?
                    route.getDroppingPoints().stream()
                            .map(routeMapper::toDroppingResponse)
                            .collect(Collectors.toList()) :
                    Collections.emptyList();

            boardingResponses.forEach(bp -> {
                if (bp.getLocationName() != null) facetBoardingPoints.add(bp.getLocationName());
            });
            droppingResponses.forEach(dp -> {
                if (dp.getLocationName() != null) facetDroppingPoints.add(dp.getLocationName());
            });

            // Available seats count
            long availableSeats = inventoryRepository.countAvailableSeatsByScheduleId(schedule.getId());

            // Duration in minutes
            int durationMinutes = route.getEstimatedDurationMinutes() != null ?
                    route.getEstimatedDurationMinutes() :
                    (int) Duration.between(schedule.getDepartureTime(), schedule.getArrivalTime()).toMinutes();

            BusSearchResultCardResponse card = BusSearchResultCardResponse.builder()
                    .scheduleId(schedule.getId())
                    .busId(schedule.getBus().getId())
                    .busNumber(schedule.getBus().getBusNumber())
                    .registrationNumber(schedule.getBus().getRegistrationNumber())
                    .busType(schedule.getBus().getBusType())
                    .amenities(schedule.getBus().getAmenities() != null ?
                            new ArrayList<>(schedule.getBus().getAmenities()) : new ArrayList<>())
                    .operatorId(schedule.getOperator().getId())
                    .operatorName(schedule.getOperator().getCompanyName())
                    .operatorCode(schedule.getOperator().getOperatorCode())
                    .operatorRating(4.5)
                    .routeId(route.getId())
                    .routeCode(route.getRouteCode())
                    .origin(route.getOrigin())
                    .destination(route.getDestination())
                    .distanceKm(route.getDistanceKm())
                    .departureTime(schedule.getDepartureTime())
                    .arrivalTime(schedule.getArrivalTime())
                    .durationMinutes(durationMinutes)
                    .startingFare(startingFare)
                    .currency("INR")
                    .availableSeatsCount(availableSeats)
                    .totalSeatsCount(schedule.getBus().getTotalSeats())
                    .boardingPoints(boardingResponses)
                    .droppingPoints(droppingResponses)
                    .scheduleStatus(schedule.getStatus())
                    .availabilityStatus(schedule.getAvailabilityStatus())
                    .build();

            allCards.add(card);
        }

        // 3. Apply Faceted Filters
        List<BusSearchResultCardResponse> filteredCards = allCards.stream()
                .filter(card -> filterByBusType(card, request.getBusType()))
                .filter(card -> filterByOperator(card, request.getOperatorId()))
                .filter(card -> filterByPrice(card, request.getMinPrice(), request.getMaxPrice()))
                .filter(card -> filterByAmenities(card, request.getAmenities()))
                .filter(card -> filterByBoardingPoint(card, request.getBoardingPoint()))
                .filter(card -> filterByDroppingPoint(card, request.getDroppingPoint()))
                .collect(Collectors.toList());

        // 4. Sort Results
        sortCards(filteredCards, request.getSortBy());

        // 5. Pagination
        int page = Math.max(0, request.getPage());
        int size = request.getSize() > 0 ? request.getSize() : 20;
        int totalElements = filteredCards.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<BusSearchResultCardResponse> pagedContent = filteredCards.subList(fromIndex, toIndex);

        BusSearchFilterSummaryResponse filterSummary = BusSearchFilterSummaryResponse.builder()
                .availableBusTypes(facetBusTypes)
                .availableOperators(facetOperators)
                .availableBoardingPoints(facetBoardingPoints)
                .availableDroppingPoints(facetDroppingPoints)
                .minPrice(overallMinPrice != null ? overallMinPrice : BigDecimal.ZERO)
                .maxPrice(overallMaxPrice != null ? overallMaxPrice : BigDecimal.ZERO)
                .totalFound(totalElements)
                .build();

        return BusSearchPageResponse.builder()
                .content(pagedContent)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .filterSummary(filterSummary)
                .build();
    }

    private BigDecimal calculateStartingFare(Schedule schedule) {
        if (schedule.getFares() != null && !schedule.getFares().isEmpty()) {
            return schedule.getFares().stream()
                    .map(Fare::getTotalAmount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        List<Fare> fares = fareRepository.findByScheduleId(schedule.getId());
        if (!fares.isEmpty()) {
            return fares.stream()
                    .map(Fare::getTotalAmount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        return BigDecimal.ZERO;
    }

    private boolean filterByBusType(BusSearchResultCardResponse card, BusType busType) {
        if (busType == null) return true;
        return card.getBusType() == busType;
    }

    private boolean filterByOperator(BusSearchResultCardResponse card, UUID operatorId) {
        if (operatorId == null) return true;
        return card.getOperatorId().equals(operatorId);
    }

    private boolean filterByPrice(BusSearchResultCardResponse card, BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && card.getStartingFare().compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && card.getStartingFare().compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    private boolean filterByAmenities(BusSearchResultCardResponse card, List<String> amenities) {
        if (amenities == null || amenities.isEmpty()) return true;
        if (card.getAmenities() == null) return false;
        return card.getAmenities().containsAll(amenities);
    }

    private boolean filterByBoardingPoint(BusSearchResultCardResponse card, String boardingPoint) {
        if (boardingPoint == null || boardingPoint.isBlank()) return true;
        String query = boardingPoint.trim().toLowerCase();
        return card.getBoardingPoints().stream().anyMatch(bp ->
                (bp.getLocationName() != null && bp.getLocationName().toLowerCase().contains(query)) ||
                (bp.getStopName() != null && bp.getStopName().toLowerCase().contains(query)) ||
                (bp.getLandmark() != null && bp.getLandmark().toLowerCase().contains(query)));
    }

    private boolean filterByDroppingPoint(BusSearchResultCardResponse card, String droppingPoint) {
        if (droppingPoint == null || droppingPoint.isBlank()) return true;
        String query = droppingPoint.trim().toLowerCase();
        return card.getDroppingPoints().stream().anyMatch(dp ->
                (dp.getLocationName() != null && dp.getLocationName().toLowerCase().contains(query)) ||
                (dp.getStopName() != null && dp.getStopName().toLowerCase().contains(query)) ||
                (dp.getLandmark() != null && dp.getLandmark().toLowerCase().contains(query)));
    }

    private void sortCards(List<BusSearchResultCardResponse> cards, String sortBy) {
        if (sortBy == null) {
            sortBy = "departure_asc";
        }

        switch (sortBy.toLowerCase()) {
            case "price_asc" -> cards.sort(Comparator.comparing(BusSearchResultCardResponse::getStartingFare));
            case "price_desc" -> cards.sort(Comparator.comparing(BusSearchResultCardResponse::getStartingFare, Comparator.reverseOrder()));
            case "departure_desc" -> cards.sort(Comparator.comparing(BusSearchResultCardResponse::getDepartureTime, Comparator.reverseOrder()));
            case "duration_asc" -> cards.sort(Comparator.comparing(c -> c.getDurationMinutes() != null ? c.getDurationMinutes() : Integer.MAX_VALUE));
            case "departure_asc" -> cards.sort(Comparator.comparing(BusSearchResultCardResponse::getDepartureTime));
            default -> cards.sort(Comparator.comparing(BusSearchResultCardResponse::getDepartureTime));
        }
    }
}
