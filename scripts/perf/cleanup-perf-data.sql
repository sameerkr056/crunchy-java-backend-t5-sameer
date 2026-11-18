-- Removes everything that seed-1000.sql created (and nothing else).
-- MySQL Workbench has "safe update mode" on by default, which blocks DELETE statements without a key.
SET SQL_SAFE_UPDATES = 0;

DELETE FROM rides WHERE passenger_name LIKE 'Perf Passenger %';
-- deleting the drivers also removes their rows in driver_vehicle_types (ON DELETE CASCADE)
DELETE FROM drivers WHERE email LIKE '%@perf.test';
DELETE FROM vehicle_types WHERE name LIKE 'Perf Type %';

SET SQL_SAFE_UPDATES = 1;

SELECT 'drivers' AS table_name, COUNT(*) AS total FROM drivers
UNION ALL SELECT 'rides', COUNT(*) FROM rides
UNION ALL SELECT 'vehicle_types', COUNT(*) FROM vehicle_types
UNION ALL SELECT 'driver_vehicle_types', COUNT(*) FROM driver_vehicle_types;
