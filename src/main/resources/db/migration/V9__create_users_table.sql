-- Application users for registration and login (Task 4).
-- The password column stores the BCrypt hash (60 characters), never the plain password.
CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    email      VARCHAR(150) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
