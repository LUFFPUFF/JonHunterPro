ALTER TABLE resumes
    ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE;

WITH ranked_resumes AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY updated_at DESC, created_at DESC
           ) AS row_number
    FROM resumes
)
UPDATE resumes
SET is_primary = TRUE
WHERE id IN (
    SELECT id
    FROM ranked_resumes
    WHERE row_number = 1
);

CREATE UNIQUE INDEX uq_resumes_primary_per_user
    ON resumes (user_id)
    WHERE is_primary = TRUE;