CREATE TABLE general_cover_letter_settings (
                                               user_id UUID PRIMARY KEY,
                                               content TEXT NOT NULL,
                                               use_when_llm_unavailable BOOLEAN NOT NULL DEFAULT FALSE,
                                               source_file_name VARCHAR(255),
                                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                               CONSTRAINT fk_general_cover_letter_settings_user
                                                   FOREIGN KEY (user_id)
                                                       REFERENCES users (id)
                                                       ON DELETE CASCADE,

                                               CONSTRAINT chk_general_cover_letter_settings_content
                                                   CHECK (
                                                       char_length(btrim(content)) BETWEEN 1 AND 4000
                                                       )
);