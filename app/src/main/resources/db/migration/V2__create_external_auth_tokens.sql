CREATE TABLE external_auth_tokens (
                                      id UUID PRIMARY KEY,
                                      user_id UUID NOT NULL,
                                      provider VARCHAR(50) NOT NULL,
                                      access_token TEXT NOT NULL,
                                      refresh_token TEXT NOT NULL,
                                      token_type VARCHAR(50) NOT NULL,
                                      scope TEXT,
                                      expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                      created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                      updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT fk_external_auth_tokens_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users (id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT uq_external_auth_tokens_user_provider
                                          UNIQUE (user_id, provider)
);

CREATE INDEX idx_external_auth_tokens_user_id
    ON external_auth_tokens (user_id);

CREATE INDEX idx_external_auth_tokens_provider
    ON external_auth_tokens (provider);

CREATE INDEX idx_external_auth_tokens_expires_at
    ON external_auth_tokens (expires_at);