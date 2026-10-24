CREATE DATABASE IF NOT EXISTS ecommerce;

CREATE TABLE IF NOT EXISTS ecommerce.users (
    id_user BIGINT NOT NULL AUTO_INCREMENT,
    user_identifier VARCHAR(20) NULL,
    name VARCHAR(100) NOT NULL,
    paternal_lastname VARCHAR(100) NOT NULL,
    maternal_lastname VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(45) NOT NULL DEFAULT 'USUARIO',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    jwt_id VARCHAR(36) NULL,
    jwt_expires_at DATETIME(6) NULL,
    refresh_token_hash VARCHAR(64) NULL,
    refresh_token_expires_at DATETIME(6) NULL,

    PRIMARY KEY (id_user),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_refresh_token_hash UNIQUE (refresh_token_hash),
    CONSTRAINT uk_users_user_identifier UNIQUE (user_identifier)
) ENGINE = InnoDB;
