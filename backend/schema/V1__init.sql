-- Vben Admin 5 + Spring Boot template, MySQL 8.4
-- Apply with Flyway. All timestamps are written in UTC by the application.

CREATE TABLE sys_dept (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    dept_code VARCHAR(64) NOT NULL,
    dept_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dept_code (dept_code),
    KEY idx_sys_dept_parent_sort (parent_id, sort_order),
    CONSTRAINT fk_sys_dept_parent FOREIGN KEY (parent_id) REFERENCES sys_dept (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(128) NOT NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(32) NULL,
    avatar_url VARCHAR(1024) NULL,
    primary_dept_id BIGINT NULL,
    home_path VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    must_change_password TINYINT NOT NULL DEFAULT 0,
    password_change_time DATETIME(3) NULL,
    last_login_time DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_email (email),
    KEY idx_sys_user_dept_status (primary_dept_id, status),
    CONSTRAINT fk_sys_user_primary_dept FOREIGN KEY (primary_dept_id) REFERENCES sys_dept (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    data_scope_type VARCHAR(32) NOT NULL DEFAULT 'SELF',
    status TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role (role_id, user_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_resource (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    resource_type VARCHAR(16) NOT NULL,
    resource_key VARCHAR(128) NOT NULL,
    route_name VARCHAR(128) NULL,
    route_path VARCHAR(255) NULL,
    component VARCHAR(255) NULL,
    redirect_path VARCHAR(255) NULL,
    permission_code VARCHAR(128) NULL,
    title_key VARCHAR(255) NOT NULL,
    icon VARCHAR(128) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    route_meta JSON NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_resource_key (resource_key),
    UNIQUE KEY uk_sys_resource_route_name (route_name),
    UNIQUE KEY uk_sys_resource_permission_code (permission_code),
    KEY idx_sys_resource_parent_sort (parent_id, sort_order),
    CONSTRAINT fk_sys_resource_parent FOREIGN KEY (parent_id) REFERENCES sys_resource (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_resource (
    role_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, resource_id),
    KEY idx_sys_role_resource_resource (resource_id, role_id),
    CONSTRAINT fk_sys_role_resource_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_resource_resource FOREIGN KEY (resource_id) REFERENCES sys_resource (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_dept_scope (
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id),
    KEY idx_sys_role_dept_scope_dept (dept_id, role_id),
    CONSTRAINT fk_sys_role_dept_scope_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_dept_scope_dept FOREIGN KEY (dept_id) REFERENCES sys_dept (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    refresh_token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    device_info VARCHAR(500) NULL,
    ip_address VARCHAR(45) NULL,
    expire_time DATETIME(3) NOT NULL,
    revoke_time DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_session_token_hash (refresh_token_hash),
    KEY idx_sys_session_user_expires (user_id, expire_time),
    CONSTRAINT fk_sys_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_login_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    username VARCHAR(64) NOT NULL,
    success TINYINT NOT NULL,
    failure_reason VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    occur_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sys_login_log_user_time (user_id, occur_time),
    KEY idx_sys_login_log_username_time (username, occur_time),
    CONSTRAINT fk_sys_login_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    action_code VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id VARCHAR(128) NULL,
    request_id VARCHAR(64) NULL,
    success TINYINT NOT NULL,
    ip_address VARCHAR(45) NULL,
    occur_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sys_operation_log_actor_time (actor_user_id, occur_time),
    KEY idx_sys_operation_log_action_time (action_code, occur_time),
    CONSTRAINT fk_sys_operation_log_actor FOREIGN KEY (actor_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
