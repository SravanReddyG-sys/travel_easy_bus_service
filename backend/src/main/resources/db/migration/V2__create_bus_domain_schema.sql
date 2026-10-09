-- ====================================================================
-- TravelEasy Bus Microservice - Complete Domain Schema Migration (V2)
-- ====================================================================

-- 1. Bus Operators Table
CREATE TABLE IF NOT EXISTS bus_operators (
    id UUID PRIMARY KEY,
    operator_code VARCHAR(50) NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(25) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_operator_code UNIQUE (operator_code)
);

CREATE INDEX IF NOT EXISTS idx_operator_active ON bus_operators(is_active);

-- 2. Buses Table
CREATE TABLE IF NOT EXISTS buses (
    id UUID PRIMARY KEY,
    operator_id UUID NOT NULL,
    bus_number VARCHAR(50) NOT NULL,
    registration_number VARCHAR(50) NOT NULL,
    bus_type VARCHAR(30) NOT NULL,
    total_seats INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bus_operator FOREIGN KEY (operator_id) REFERENCES bus_operators(id) ON DELETE CASCADE,
    CONSTRAINT uk_bus_operator_number UNIQUE (operator_id, bus_number),
    CONSTRAINT uk_bus_registration UNIQUE (registration_number)
);

CREATE INDEX IF NOT EXISTS idx_buses_operator_id ON buses(operator_id);
CREATE INDEX IF NOT EXISTS idx_buses_status ON buses(status);
CREATE INDEX IF NOT EXISTS idx_buses_type ON buses(bus_type);

-- 3. Bus Amenities Table (ElementCollection)
CREATE TABLE IF NOT EXISTS bus_amenities (
    bus_id UUID NOT NULL,
    amenity VARCHAR(100) NOT NULL,
    CONSTRAINT fk_amenity_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_amenities_bus_id ON bus_amenities(bus_id);

-- 4. Physical Seats Table
CREATE TABLE IF NOT EXISTS seats (
    id UUID PRIMARY KEY,
    bus_id UUID NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    deck VARCHAR(10) NOT NULL DEFAULT 'LOWER',
    row_num INTEGER NOT NULL,
    col_num INTEGER NOT NULL,
    seat_type VARCHAR(20) NOT NULL DEFAULT 'SEATER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE CASCADE,
    CONSTRAINT uk_bus_seat_number UNIQUE (bus_id, seat_number)
);

CREATE INDEX IF NOT EXISTS idx_seats_bus_id ON seats(bus_id);
CREATE INDEX IF NOT EXISTS idx_seats_deck ON seats(deck);
CREATE INDEX IF NOT EXISTS idx_seats_type ON seats(seat_type);

-- 5. Routes Table
CREATE TABLE IF NOT EXISTS routes (
    id UUID PRIMARY KEY,
    operator_id UUID NOT NULL,
    route_code VARCHAR(50) NOT NULL,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_operator FOREIGN KEY (operator_id) REFERENCES bus_operators(id) ON DELETE CASCADE,
    CONSTRAINT uk_route_operator_code UNIQUE (operator_id, route_code)
);

CREATE INDEX IF NOT EXISTS idx_routes_operator_id ON routes(operator_id);
CREATE INDEX IF NOT EXISTS idx_routes_search ON routes(origin, destination, is_active);
CREATE INDEX IF NOT EXISTS idx_routes_active ON routes(is_active);

-- 6. Route Stops Table
CREATE TABLE IF NOT EXISTS route_stops (
    id UUID PRIMARY KEY,
    route_id UUID NOT NULL,
    stop_name VARCHAR(100) NOT NULL,
    stop_order INTEGER NOT NULL,
    distance_from_origin_km DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stop_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    CONSTRAINT uk_route_stop_order UNIQUE (route_id, stop_order)
);

CREATE INDEX IF NOT EXISTS idx_route_stops_route_id ON route_stops(route_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_order ON route_stops(route_id, stop_order);

-- 7. Boarding Points Table
CREATE TABLE IF NOT EXISTS boarding_points (
    id UUID PRIMARY KEY,
    route_id UUID NOT NULL,
    stop_id UUID,
    location_name VARCHAR(150) NOT NULL,
    landmark VARCHAR(200),
    departure_offset_minutes INTEGER NOT NULL DEFAULT 0,
    contact_phone VARCHAR(25),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_boarding_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    CONSTRAINT fk_boarding_stop FOREIGN KEY (stop_id) REFERENCES route_stops(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_boarding_route_id ON boarding_points(route_id);
CREATE INDEX IF NOT EXISTS idx_boarding_stop_id ON boarding_points(stop_id);

-- 8. Dropping Points Table
CREATE TABLE IF NOT EXISTS dropping_points (
    id UUID PRIMARY KEY,
    route_id UUID NOT NULL,
    stop_id UUID,
    location_name VARCHAR(150) NOT NULL,
    landmark VARCHAR(200),
    arrival_offset_minutes INTEGER NOT NULL DEFAULT 0,
    contact_phone VARCHAR(25),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dropping_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    CONSTRAINT fk_dropping_stop FOREIGN KEY (stop_id) REFERENCES route_stops(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_dropping_route_id ON dropping_points(route_id);
CREATE INDEX IF NOT EXISTS idx_dropping_stop_id ON dropping_points(stop_id);

-- 9. Schedules Table
CREATE TABLE IF NOT EXISTS schedules (
    id UUID PRIMARY KEY,
    bus_id UUID NOT NULL,
    route_id UUID NOT NULL,
    operator_id UUID NOT NULL,
    schedule_date DATE NOT NULL,
    departure_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    arrival_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_schedule_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_schedule_operator FOREIGN KEY (operator_id) REFERENCES bus_operators(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_schedules_bus_date ON schedules(bus_id, schedule_date);
CREATE INDEX IF NOT EXISTS idx_schedules_route_date ON schedules(route_id, schedule_date);
CREATE INDEX IF NOT EXISTS idx_schedules_operator_id ON schedules(operator_id);
CREATE INDEX IF NOT EXISTS idx_schedules_status_avail ON schedules(status, availability_status);
CREATE INDEX IF NOT EXISTS idx_schedules_departure ON schedules(departure_time);

-- 10. Schedule Seat Inventory Table (Schedule-specific dynamic availability & locking)
CREATE TABLE IF NOT EXISTS schedule_seat_inventory (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    lock_expiry_time TIMESTAMP WITHOUT TIME ZONE,
    locked_by_booking_session VARCHAR(100),
    booking_reference VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE RESTRICT,
    CONSTRAINT uk_schedule_seat_inventory UNIQUE (schedule_id, seat_id)
);

CREATE INDEX IF NOT EXISTS idx_sched_inv_schedule_id ON schedule_seat_inventory(schedule_id);
CREATE INDEX IF NOT EXISTS idx_sched_inv_status ON schedule_seat_inventory(schedule_id, status);
CREATE INDEX IF NOT EXISTS idx_sched_inv_expiry ON schedule_seat_inventory(status, lock_expiry_time);
CREATE INDEX IF NOT EXISTS idx_sched_inv_booking_ref ON schedule_seat_inventory(booking_reference);

-- 11. Fares Table
CREATE TABLE IF NOT EXISTS fares (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    base_amount NUMERIC(10, 2) NOT NULL,
    tax_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fare_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE,
    CONSTRAINT uk_fare_schedule_seat_type UNIQUE (schedule_id, seat_type)
);

CREATE INDEX IF NOT EXISTS idx_fares_schedule_id ON fares(schedule_id);

-- 12. Bus Availability Override Log Table
CREATE TABLE IF NOT EXISTS bus_availability (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    modified_by VARCHAR(100),
    effective_from TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avail_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bus_avail_schedule ON bus_availability(schedule_id);

-- 13. Transactional Outbox for Domain Events
CREATE TABLE IF NOT EXISTS bus_events_outbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    correlation_id VARCHAR(100),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON bus_events_outbox(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON bus_events_outbox(aggregate_id, event_type);
