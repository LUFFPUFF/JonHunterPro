CREATE TABLE auto_response_queue_items (
                                           id UUID PRIMARY KEY,
                                           user_id UUID NOT NULL,
                                           source VARCHAR(50) NOT NULL,
                                           external_vacancy_id VARCHAR(100) NOT NULL,
                                           vacancy_name TEXT NOT NULL,
                                           employer_name TEXT,
                                           area_name TEXT,
                                           vacancy_url TEXT,
                                           status VARCHAR(50) NOT NULL,
                                           created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                           CONSTRAINT fk_auto_response_queue_items_user
                                               FOREIGN KEY (user_id)
                                                   REFERENCES users (id)
                                                   ON DELETE CASCADE,

                                           CONSTRAINT uq_auto_response_queue_user_source_external_vacancy
                                               UNIQUE (user_id, source, external_vacancy_id)
);

CREATE INDEX idx_auto_response_queue_items_user_id
    ON auto_response_queue_items (user_id);

CREATE INDEX idx_auto_response_queue_items_status
    ON auto_response_queue_items (status);

CREATE INDEX idx_auto_response_queue_items_source
    ON auto_response_queue_items (source);