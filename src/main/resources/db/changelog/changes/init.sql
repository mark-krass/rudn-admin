CREATE TABLE rudn_user
(
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR NOT NULL UNIQUE,
    name        VARCHAR,
    middle_name VARCHAR,
    lastname    VARCHAR,
    birthday    DATE
);

CREATE TABLE direction_dictionary
(
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR NOT NULL UNIQUE,
    code      VARCHAR NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE group_dictionary
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR                                        NOT NULL UNIQUE,
    year         SMALLINT CHECK (year >= 1900 AND year <= 2200) NOT NULL,
    direction_id BIGINT                                         NOT NULL,
    CONSTRAINT fk_group_direction
        FOREIGN KEY (direction_id)
            REFERENCES direction_dictionary (id)
);

CREATE TABLE student
(
    id       BIGSERIAL PRIMARY KEY,
    group_id BIGINT        NOT NULL,
    user_id  BIGINT UNIQUE NOT NULL,
    CONSTRAINT fk_student_group
        FOREIGN KEY (group_id)
            REFERENCES group_dictionary (id),
    CONSTRAINT fk_student_user
        FOREIGN KEY (user_id)
            REFERENCES rudn_user (id)

);

CREATE TABLE vpn
(
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    link    VARCHAR,
    CONSTRAINT fk_vpn_user
        FOREIGN KEY (user_id)
            REFERENCES rudn_user (id)
);

CREATE TYPE task_type AS ENUM ('CREATE_OVPN', 'UPLOAD_MINIO', 'RENEW_LINK', 'DELETE_MINIO', 'DELETE_OVPN');
CREATE TABLE vpn_task
(
    id        BIGSERIAL PRIMARY KEY,
    vpn_id    BIGINT    NOT NULL,
    type      task_type NOT NULL,
    is_active BOOLEAN   NOT NULL DEFAULT TRUE,
    error     VARCHAR,
    CONSTRAINT fk_vpn_task_vpn
        FOREIGN KEY (vpn_id)
            REFERENCES vpn (id)
            ON DELETE CASCADE
);
-- Only one active task per VPN at a time
CREATE UNIQUE INDEX IF NOT EXISTS ux_vpn_task_active_vpn
    ON vpn_task (vpn_id)
    WHERE is_active = TRUE;