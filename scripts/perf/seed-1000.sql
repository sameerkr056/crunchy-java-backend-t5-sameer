-- Creates test data for the performance test: 1,000 drivers, 3,000 rides, 1,000 vehicle types, 2,000 links.
-- Run it in MySQL Workbench (File -> Open SQL Script -> lightning bolt) on the database you want to test:
--   * local:   connection to your local MySQL, schema cabbooking_db
--   * Railway: connection to the Railway MySQL (see README, "Inspect Railway MySQL with MySQL Workbench")
-- Everything created here is easy to recognise (names start with "Perf", emails end with @perf.test)
-- and can be removed again with cleanup-perf-data.sql.
-- This is NOT a Flyway migration on purpose: test data must never be part of the production schema history.

SET SESSION cte_max_recursion_depth = 5000;

-- 1) 1,000 drivers (all available, so the data follows the business rules)
INSERT INTO drivers (name, email, phone, license_number, available)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT CONCAT('Perf Driver ', n),
       CONCAT('perf.driver', n, '@perf.test'),
       CONCAT('9', LPAD(n, 9, '0')),
       CONCAT('PERF-LIC-', LPAD(n, 5, '0')),
       1
FROM seq;

-- 2) 1,000 vehicle types
INSERT INTO vehicle_types (name, description)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT CONCAT('Perf Type ', n), CONCAT('Generated vehicle type ', n)
FROM seq;

-- 3) 3,000 rides, 3 per driver (REQUESTED, COMPLETED or CANCELLED only, so no driver is blocked)
INSERT INTO rides (passenger_name, pickup_location, drop_location, fare, status, booking_time, driver_id)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 3000
)
SELECT CONCAT('Perf Passenger ', seq.n),
       ELT(1 + (seq.n % 5), 'Station', 'Airport', 'City Centre', 'Hirapur', 'Bank More'),
       ELT(1 + ((seq.n + 2) % 5), 'Station', 'Airport', 'City Centre', 'Hirapur', 'Bank More'),
       50 + (seq.n * 37) % 450 + 0.50,
       ELT(1 + (seq.n % 3), 'COMPLETED', 'CANCELLED', 'REQUESTED'),
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL seq.n HOUR,
       d.id
FROM seq
JOIN drivers d ON d.email = CONCAT('perf.driver', 1 + ((seq.n - 1) % 1000), '@perf.test');

-- 4) 2,000 driver <-> vehicle type links (2 per driver)
INSERT INTO driver_vehicle_types (driver_id, vehicle_type_id)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT d.id, v.id
FROM seq
JOIN drivers d ON d.email = CONCAT('perf.driver', seq.n, '@perf.test')
JOIN vehicle_types v ON v.name = CONCAT('Perf Type ', seq.n)
UNION ALL
SELECT d.id, v.id
FROM seq
JOIN drivers d ON d.email = CONCAT('perf.driver', seq.n, '@perf.test')
JOIN vehicle_types v ON v.name = CONCAT('Perf Type ', (seq.n % 1000) + 1);

-- 5) Check the result. Expected: drivers = 25 + 1000, rides = 75 + 3000, vehicle_types = 7 + 1000 (if the sample data is still there)
SELECT 'drivers' AS table_name, COUNT(*) AS total FROM drivers
UNION ALL SELECT 'rides', COUNT(*) FROM rides
UNION ALL SELECT 'vehicle_types', COUNT(*) FROM vehicle_types
UNION ALL SELECT 'driver_vehicle_types', COUNT(*) FROM driver_vehicle_types;
