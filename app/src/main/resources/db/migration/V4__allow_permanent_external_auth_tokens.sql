ALTER TABLE external_auth_tokens
    ALTER COLUMN refresh_token DROP NOT NULL;

ALTER TABLE external_auth_tokens
    ALTER COLUMN expires_at DROP NOT NULL;