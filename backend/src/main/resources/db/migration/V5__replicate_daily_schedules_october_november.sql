-- ====================================================================
-- TravelEasy Bus Microservice - Migration V5
-- Replicate 100 Schedules Across Every Date of October & November 2026
-- Ensures buses are available for any date selected (Oct 15 to Nov 15)
-- ====================================================================

DO $$
DECLARE
    target_date DATE;
    s RECORD;
    new_sched_id UUID;
BEGIN
    FOR target_date IN 
        SELECT d::date FROM generate_series('2026-10-15'::date, '2026-11-15'::date, '1 day'::interval) d
    LOOP
        FOR s IN 
            SELECT id, bus_id, route_id, operator_id, 
                   departure_time::time AS dep_time, 
                   arrival_time::time AS arr_time,
                   (arrival_time < departure_time) AS overnight
            FROM schedules 
            WHERE schedule_date = '2026-10-10'
        LOOP
            new_sched_id := gen_random_uuid();
            
            INSERT INTO schedules (
                id, bus_id, route_id, operator_id, schedule_date,
                departure_time, arrival_time, status, availability_status,
                created_at, updated_at
            ) VALUES (
                new_sched_id, s.bus_id, s.route_id, s.operator_id, target_date,
                (target_date + s.dep_time),
                (target_date + s.arr_time + CASE WHEN s.overnight THEN INTERVAL '1 day' ELSE INTERVAL '0 day' END),
                'SCHEDULED', 'AVAILABLE',
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            );

            INSERT INTO schedule_seat_inventory (id, schedule_id, seat_id, status, created_at, updated_at)
            SELECT gen_random_uuid(), new_sched_id, seats.id, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM seats
            WHERE seats.bus_id = s.bus_id;

            INSERT INTO fares (id, schedule_id, seat_type, base_amount, tax_amount, discount_amount, total_amount, currency, created_at, updated_at)
            SELECT gen_random_uuid(), new_sched_id, fares.seat_type, fares.base_amount, fares.tax_amount, fares.discount_amount, fares.total_amount, fares.currency, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM fares
            WHERE fares.schedule_id = s.id;
        END LOOP;
    END LOOP;
END $$;
