-- Run these in MySQL Workbench AFTER seed-1000.sql to see whether MySQL uses the indexes.
-- Look at the columns "key" (the index used) and "rows" (rows MySQL expects to read).
-- Copy the interesting rows into docs/PERFORMANCE.md.

-- Drivers: filter by availability, sorted by name (first page of 20)
EXPLAIN SELECT * FROM drivers WHERE available = 1 ORDER BY name, id LIMIT 20;

-- Drivers: plain first page sorted by id
EXPLAIN SELECT * FROM drivers ORDER BY id LIMIT 20;

-- Rides: newest first
EXPLAIN SELECT * FROM rides ORDER BY booking_time DESC, id LIMIT 20;

-- Rides: filter by status, newest first
EXPLAIN SELECT * FROM rides WHERE status = 'COMPLETED' ORDER BY booking_time DESC, id LIMIT 20;

-- Rides of one driver with a status (also used by the "is the driver on an ACCEPTED ride?" check)
EXPLAIN SELECT 1 FROM rides WHERE driver_id = 10 AND status = 'ACCEPTED' LIMIT 1;

-- N+1 fix: all rides of 20 drivers in ONE query
EXPLAIN SELECT d.*, r.* FROM drivers d LEFT JOIN rides r ON r.driver_id = d.id
WHERE d.id IN (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20);

-- Vehicle types of one driver (many-to-many)
EXPLAIN SELECT v.* FROM vehicle_types v JOIN driver_vehicle_types l ON l.vehicle_type_id = v.id WHERE l.driver_id = 10;

-- Drivers of one vehicle type (many-to-many, other direction)
EXPLAIN SELECT d.* FROM drivers d JOIN driver_vehicle_types l ON l.driver_id = d.id WHERE l.vehicle_type_id = 10;
