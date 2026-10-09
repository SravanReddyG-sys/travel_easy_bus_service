package com.traveleasy.bus.controller;

import com.traveleasy.bus.dto.response.ApiResponse;
import com.traveleasy.bus.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service Health Check Endpoints")
public class HealthController {

    @Value("${spring.application.name:traveleasy-bus-service}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping
    @Operation(summary = "Check Bus Service Health", description = "Returns the operational status and metadata of the bus microservice.")
    public ResponseEntity<ApiResponse<HealthResponse>> getHealth() {
        Map<String, String> details = new HashMap<>();
        details.put("profile", activeProfile);
        details.put("javaVersion", System.getProperty("java.version"));
        details.put("dbEngine", "PostgreSQL");

        HealthResponse health = HealthResponse.builder()
                .status("UP")
                .service(applicationName)
                .version("1.0.0-SNAPSHOT")
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Bus microservice is healthy and operational", health));
    }
}
