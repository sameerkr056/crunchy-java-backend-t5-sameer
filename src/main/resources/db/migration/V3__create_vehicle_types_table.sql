-- Vehicle types: Sedan, SUV, ...
CREATE TABLE vehicle_types (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vehicle_types_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
