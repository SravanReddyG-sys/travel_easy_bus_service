# generate_seed.py: Clean RFC4122 UUID 10 Routes x 10 Buses Generator

operators = [
    {"idx": 1, "code": "OP-ROYAL-01", "name": "Royal Express Travels", "email": "royal@traveleasy.com", "phone": "+91-9876543210"},
    {"idx": 2, "code": "OP-ZING-02", "name": "ZingBus Premium Lines", "email": "zing@traveleasy.com", "phone": "+91-9811223344"},
    {"idx": 3, "code": "OP-KALLADA-03", "name": "Kallada Travels", "email": "kallada@traveleasy.com", "phone": "+91-9447788990"},
    {"idx": 4, "code": "OP-GREEN-04", "name": "GreenLine Express", "email": "greenline@traveleasy.com", "phone": "+91-9822334455"},
    {"idx": 5, "code": "OP-VRL-05", "name": "VRL Logistics & Travels", "email": "vrl@traveleasy.com", "phone": "+91-9844112233"},
    {"idx": 6, "code": "OP-SRS-06", "name": "SRS Travels Super Deluxe", "email": "srs@traveleasy.com", "phone": "+91-9845001122"},
    {"idx": 7, "code": "OP-ORANGE-07", "name": "Orange Tours & Travels", "email": "orange@traveleasy.com", "phone": "+91-9866003344"},
    {"idx": 8, "code": "OP-NEETA-08", "name": "Neeta Tours & Travels", "email": "neeta@traveleasy.com", "phone": "+91-9820005566"},
    {"idx": 9, "code": "OP-INTRCITY-09", "name": "IntrCity SmartBus Lines", "email": "intrcity@traveleasy.com", "phone": "+91-9810007788"},
    {"idx": 10, "code": "OP-CHARTERED-10", "name": "Chartered Speed Bus", "email": "chartered@traveleasy.com", "phone": "+91-9826009900"},
]

routes = [
    {"index": 1, "origin": "Delhi", "dest": "Jaipur", "dist": 280.0, "duration": 330, "b_name": "ISBT Kashmiri Gate", "b_land": "Metro Gate 1", "d_name": "Sindhi Camp Jaipur", "d_land": "Main Bus Stand"},
    {"index": 2, "origin": "Delhi", "dest": "Chandigarh", "dist": 250.0, "duration": 270, "b_name": "Majnu Ka Tilla", "b_land": "HP Petrol Pump", "d_name": "Sector 43 Bus Stand", "d_land": "Main Terminal"},
    {"index": 3, "origin": "Mumbai", "dest": "Pune", "dist": 150.0, "duration": 210, "b_name": "Borivali West", "b_land": "Gokul Hotel", "d_name": "Swargate Pune", "d_land": "Laxmi Narayan Theater"},
    {"index": 4, "origin": "Bangalore", "dest": "Chennai", "dist": 350.0, "duration": 360, "b_name": "Madiwala", "b_land": "Near Police Station", "d_name": "Koyambedu CMBT", "d_land": "Platform 5"},
    {"index": 5, "origin": "Hyderabad", "dest": "Vijayawada", "dist": 275.0, "duration": 300, "b_name": "MGBS Hyderabad", "b_land": "Main Terminal", "d_name": "Pandit Nehru Bus Stand", "d_land": "City Gate"},
    {"index": 6, "origin": "Bangalore", "dest": "Hyderabad", "dist": 570.0, "duration": 540, "b_name": "Majestic Bus Station", "b_land": "Platform 10", "d_name": "Afzalgunj Hyderabad", "d_land": "Near Flyover"},
    {"index": 7, "origin": "Mumbai", "dest": "Goa", "dist": 590.0, "duration": 660, "b_name": "Vashi Plaza", "b_land": "Highway Junction", "d_name": "Panjim Bus Terminal", "d_land": "Mandovi River Gate"},
    {"index": 8, "origin": "Chennai", "dest": "Coimbatore", "dist": 500.0, "duration": 480, "b_name": "Guindy Kathipara", "b_land": "Metro Station", "d_name": "Gandhipuram Bus Stand", "d_land": "Clock Tower"},
    {"index": 9, "origin": "Pune", "dest": "Goa", "dist": 450.0, "duration": 510, "b_name": "Katraj Bus Stand", "b_land": "Highway Bypass", "d_name": "Mapusa Bus Stand", "d_land": "Main Market"},
    {"index": 10, "origin": "Delhi", "dest": "Agra", "dist": 210.0, "duration": 210, "b_name": "Sarai Kale Khan", "b_land": "ISBT Entry", "d_name": "Idgah Bus Stand Agra", "d_land": "Station Road"}
]

bus_types = ["AC_SEATER", "AC_SLEEPER", "VOLVO_MULTI_AXLE", "NON_AC_SEATER", "AC_SLEEPER"]
times = [
    ("05:30:00", 450.0),
    ("07:00:00", 550.0),
    ("09:30:00", 600.0),
    ("11:45:00", 500.0),
    ("14:15:00", 550.0),
    ("16:30:00", 650.0),
    ("18:45:00", 750.0),
    ("20:30:00", 850.0),
    ("22:15:00", 950.0),
    ("23:45:00", 900.0)
]

sql = []
sql.append("-- V3__seed_demo_data.sql: 10 Routes x 10 Buses = 100 Schedules Fleet Dataset\n")
sql.append("TRUNCATE TABLE schedule_seat_inventory, fares, schedules, seats, bus_amenities, boarding_points, dropping_points, routes, buses, bus_operators CASCADE;\n")

# 1. Operators
sql.append("-- 1. Insert 10 Bus Operators")
for op in operators:
    op_id = f"00000000-0000-0000-0000-{op['idx']:012d}"
    sql.append(f"INSERT INTO bus_operators (id, operator_code, company_name, contact_email, contact_phone, is_active, created_at, updated_at) "
               f"VALUES ('{op_id}', '{op['code']}', '{op['name']}', '{op['email']}', '{op['phone']}', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
               f"ON CONFLICT (operator_code) DO NOTHING;")

# 2. Routes & Stops
sql.append("\n-- 2. Insert 10 Intercity Routes & Boarding/Dropping Points")
for r in routes:
    r_idx = r['index']
    r_id = f"00000000-0000-0000-0002-{r_idx:012d}"
    op = operators[(r_idx - 1) % len(operators)]
    op_id = f"00000000-0000-0000-0000-{op['idx']:012d}"
    code = f"RT-{r['origin'][:3].upper()}-{r['dest'][:3].upper()}-{r_idx:02d}"
    
    sql.append(f"INSERT INTO routes (id, operator_id, route_code, origin, destination, distance_km, estimated_duration_minutes, is_active, created_at, updated_at) "
               f"VALUES ('{r_id}', '{op_id}', '{code}', '{r['origin']}', '{r['dest']}', {r['dist']}, {r['duration']}, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
               f"ON CONFLICT (operator_id, route_code) DO NOTHING;")
    
    bp_id = f"00000000-0000-0000-0003-{r_idx:012d}"
    sql.append(f"INSERT INTO boarding_points (id, route_id, location_name, landmark, departure_offset_minutes, created_at, updated_at) "
               f"VALUES ('{bp_id}', '{r_id}', '{r['b_name']}', '{r['b_land']}', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;")
    
    dp_id = f"00000000-0000-0000-0004-{r_idx:012d}"
    sql.append(f"INSERT INTO dropping_points (id, route_id, location_name, landmark, arrival_offset_minutes, created_at, updated_at) "
               f"VALUES ('{dp_id}', '{r_id}', '{r['d_name']}', '{r['d_land']}', {r['duration']}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;")

# 3. 100 Buses, Seats, Schedules, Inventory & Fares (10 per route)
sql.append("\n-- 3. Insert 100 Buses, Seats, Schedules, Inventory & Fares (10 per route)")
bus_counter = 0

for r in routes:
    r_idx = r['index']
    r_id = f"00000000-0000-0000-0002-{r_idx:012d}"
    
    for b_idx in range(1, 11):
        bus_counter += 1
        op = operators[(bus_counter - 1) % len(operators)]
        op_id = f"00000000-0000-0000-0000-{op['idx']:012d}"
        b_type = bus_types[(bus_counter - 1) % len(bus_types)]
        
        bus_id = f"00000000-0000-0000-0001-{bus_counter:012d}"
        bus_num = f"{op['name'].split()[0].upper()}-{r['origin'][:3].upper()}{r['dest'][:3].upper()}-{b_idx:02d}"
        reg_num = f"IN-{r_idx:02d}-{op['idx']:02d}-{b_idx:04d}"
        
        # Insert Bus
        sql.append(f"INSERT INTO buses (id, operator_id, bus_number, registration_number, bus_type, total_seats, status, created_at, updated_at) "
                   f"VALUES ('{bus_id}', '{op_id}', '{bus_num}', '{reg_num}', '{b_type}', 6, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                   f"ON CONFLICT (registration_number) DO NOTHING;")
        
        # Amenities
        sql.append(f"INSERT INTO bus_amenities (bus_id, amenity) VALUES ('{bus_id}', 'WiFi'), ('{bus_id}', 'Charging Point'), ('{bus_id}', 'Water Bottle'), ('{bus_id}', 'Blanket') ON CONFLICT DO NOTHING;")
        
        # 6 Seats per bus
        is_sleeper = "SLEEPER" in b_type
        seats = [
            ("1A", "LOWER", "SLEEPER" if is_sleeper else "SEATER", 1, 1),
            ("1B", "LOWER", "SLEEPER" if is_sleeper else "SEATER", 1, 2),
            ("2A", "LOWER", "SLEEPER" if is_sleeper else "SEATER", 2, 1),
            ("2B", "LOWER", "SLEEPER" if is_sleeper else "SEATER", 2, 2),
            ("U1" if is_sleeper else "3A", "UPPER" if is_sleeper else "LOWER", "SLEEPER" if is_sleeper else "SEATER", 1 if is_sleeper else 3, 1),
            ("U2" if is_sleeper else "3B", "UPPER" if is_sleeper else "LOWER", "SLEEPER" if is_sleeper else "SEATER", 1 if is_sleeper else 3, 2),
        ]
        
        seat_ids = []
        for s_idx, (s_num, deck, s_type, row, col) in enumerate(seats):
            seat_id = f"00000000-0000-0001-{bus_counter:04d}-{(s_idx+1):012d}"
            seat_ids.append(seat_id)
            sql.append(f"INSERT INTO seats (id, bus_id, seat_number, deck, row_num, col_num, seat_type, is_active, created_at, updated_at) "
                       f"VALUES ('{seat_id}', '{bus_id}', '{s_num}', '{deck}', {row}, {col}, '{s_type}', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                       f"ON CONFLICT (bus_id, seat_number) DO NOTHING;")
        
        # Schedule for tomorrow
        sched_id = f"00000000-0000-0002-{bus_counter:04d}-{b_idx:012d}"
        dep_time_str, base_fare = times[(b_idx - 1) % len(times)]
        
        sql.append(f"INSERT INTO schedules (id, bus_id, route_id, operator_id, schedule_date, departure_time, arrival_time, status, availability_status, created_at, updated_at) "
                   f"VALUES ('{sched_id}', '{bus_id}', '{r_id}', '{op_id}', CURRENT_DATE + INTERVAL '1 day', "
                   f"CURRENT_DATE + INTERVAL '1 day {dep_time_str}', CURRENT_DATE + INTERVAL '1 day {dep_time_str}' + INTERVAL '{r['duration']} minutes', "
                   f"'SCHEDULED', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;")
        
        # Schedule Seat Inventory
        for s_idx, seat_id in enumerate(seat_ids):
            inv_id = f"00000000-0000-0003-{bus_counter:04d}-{(s_idx+1):012d}"
            sql.append(f"INSERT INTO schedule_seat_inventory (id, schedule_id, seat_id, status, created_at, updated_at) "
                       f"VALUES ('{inv_id}', '{sched_id}', '{seat_id}', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                       f"ON CONFLICT (schedule_id, seat_id) DO NOTHING;")
        
        # Fare
        fare_id = f"00000000-0000-0004-{bus_counter:04d}-{b_idx:012d}"
        tax = round(base_fare * 0.05, 2)
        total = round(base_fare + tax, 2)
        stype = "SLEEPER" if is_sleeper else "SEATER"
        sql.append(f"INSERT INTO fares (id, schedule_id, seat_type, base_amount, tax_amount, discount_amount, total_amount, currency, created_at, updated_at) "
                   f"VALUES ('{fare_id}', '{sched_id}', '{stype}', {base_fare}, {tax}, 0.00, {total}, 'INR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                   f"ON CONFLICT (schedule_id, seat_type) DO NOTHING;")

with open("c:/Users/sneha/Downloads/saa project/bus-service/backend/src/main/resources/db/migration/V3__seed_demo_data.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql))

print(f"Generated clean RFC4122 seed file for 10 routes and {bus_counter} schedules!")
