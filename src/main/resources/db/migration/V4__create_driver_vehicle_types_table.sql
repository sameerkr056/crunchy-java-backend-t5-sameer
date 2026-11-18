-- Join table for the many-to-many relationship between drivers and vehicle types.
-- The composite primary key prevents the same pair from being stored twice.
CREATE TABLE driver_vehicle_types (
    driver_id       BIGINT NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    PRIMARY KEY (driver_id, vehicle_type_id),
    INDEX idx_dvt_vehicle_type_id (vehicle_type_id),
    CONSTRAINT fk_dvt_driver FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE CASCADE,
    CONSTRAINT fk_dvt_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
