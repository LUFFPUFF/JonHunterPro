ALTER TABLE auto_response_queue_items
    ADD COLUMN candidate_approval_reason TEXT;

ALTER TABLE auto_response_queue_items
    ADD COLUMN diagnostic_directory TEXT;

CREATE INDEX idx_auto_response_queue_candidate_approval
    ON auto_response_queue_items (user_id, status)
    WHERE status = 'WAITING_CANDIDATE_APPROVAL';