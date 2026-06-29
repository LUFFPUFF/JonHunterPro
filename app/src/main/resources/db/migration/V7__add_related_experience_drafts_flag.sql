ALTER TABLE candidate_questionnaire_profiles
    ADD COLUMN IF NOT EXISTS allow_related_experience_drafts
    BOOLEAN NOT NULL DEFAULT FALSE;