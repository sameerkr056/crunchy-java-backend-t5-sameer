-- Task 5: indexes chosen from the real queries of the list endpoints.
-- Already present from earlier tasks (kept): unique indexes on drivers.email / license_number, vehicle_types.name,
-- users.username / email, rides.driver_id, rides.status, driver_vehicle_types(vehicle_type_id).

-- GET /drivers?available=...&sortBy=...  : filter by availability, sort by name
CREATE INDEX idx_drivers_name ON drivers (name);
CREATE INDEX idx_drivers_available_id ON drivers (available, id);

-- GET /rides?status=...&driverId=...&sortBy=bookingTime|...  and the "does this driver have an ACCEPTED ride?" check
CREATE INDEX idx_rides_driver_status ON rides (driver_id, status);
CREATE INDEX idx_rides_booking_time ON rides (booking_time);
CREATE INDEX idx_rides_status_booking_time ON rides (status, booking_time);
