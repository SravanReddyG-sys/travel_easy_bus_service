package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.request.BusSearchRequest;
import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.BusSearchPageResponse;
import com.traveleasy.bus.service.BusSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buses/search")
@RequiredArgsConstructor
@Tag(name = "Bus Search Engine", description = "Public traveller search API for searching bus schedules, fares, and seat availability")
public class BusSearchController {

    private final BusSearchService busSearchService;

    @GetMapping
    @Operation(summary = "Search available bus schedules",
               description = "Core traveller query endpoint. Filters by route, date, operator, bus type, price range, boarding/dropping points with faceted summary.")
    public ResponseEntity<ApiResponse<BusSearchPageResponse>> searchBuses(@Valid @ModelAttribute BusSearchRequest request) {
        BusSearchPageResponse response = busSearchService.searchBuses(request);
        return ResponseEntity.ok(ApiResponse.ok("Search results retrieved successfully", response));
    }
}
