package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.ConfirmSeatsRequest;
import com.traveleasy.bus.dto.request.LockSeatsRequest;
import com.traveleasy.bus.dto.request.ReleaseSeatsRequest;
import com.traveleasy.bus.dto.response.ConfirmSeatsResponse;
import com.traveleasy.bus.dto.response.LockSeatsResponse;
import com.traveleasy.bus.dto.response.ReleaseSeatsResponse;

import java.util.UUID;

public interface SeatLockService {

    LockSeatsResponse lockSeats(UUID scheduleId, LockSeatsRequest request);

    ConfirmSeatsResponse confirmSeats(UUID scheduleId, ConfirmSeatsRequest request);

    ReleaseSeatsResponse releaseSeats(UUID scheduleId, ReleaseSeatsRequest request);

    int cleanupExpiredLocks();
}
