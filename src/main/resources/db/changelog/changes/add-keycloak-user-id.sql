ALTER TABLE rudn_user
    ADD COLUMN IF NOT EXISTS keycloak_user_id VARCHAR;

CREATE UNIQUE INDEX IF NOT EXISTS ux_rudn_user_keycloak_user_id
    ON rudn_user (keycloak_user_id);
