-- Rides: many rides belong to one driver (rides.driver_id is the foreign key = owning side of the relationship)
CREATE TABLE rides (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    passenger_name  VARCHAR(100)   NOT NULL,
    pickup_location VARCHAR(255)   NOT NULL,
    drop_location   VARCHAR(255)   NOT NULL,
    fare            DECIMAL(10, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    booking_time    DATETIME(6)    NOT NULL,
    driver_id       BIGINT         NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_rides_driver_id (driver_id),
    INDEX idx_rides_status (status),
    CONSTRAINT fk_rides_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT chk_rides_status CHECK (status IN ('REQUESTED', 'ACCEPTED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_rides_fare CHECK (fare > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
