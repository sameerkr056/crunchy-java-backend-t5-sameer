-- Drivers: one row per driver
CREATE TABLE drivers (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    phone          VARCHAR(15)  NOT NULL,
    license_number VARCHAR(50)  NOT NULL,
    available      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_drivers_email UNIQUE (email),
    CONSTRAINT uk_drivers_license_number UNIQUE (license_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
