ALTER TABLE candidate_questionnaire_profiles
    RENAME COLUMN timezone_id TO time_zone_id;

ALTER TABLE candidate_questionnaire_profiles
ALTER COLUMN salary_min TYPE NUMERIC(12, 2)
    USING salary_min::NUMERIC(12, 2);

ALTER TABLE candidate_questionnaire_profiles
ALTER COLUMN salary_max TYPE NUMERIC(12, 2)
    USING salary_max::NUMERIC(12, 2);

ALTER TABLE candidate_questionnaire_profiles
    ADD COLUMN IF NOT EXISTS salary_tax_basis
    VARCHAR(20) NOT NULL DEFAULT 'UNSPECIFIED';

ALTER TABLE candidate_questionnaire_profiles
DROP CONSTRAINT IF EXISTS chk_candidate_questionnaire_profile_relocation;

ALTER TABLE candidate_questionnaire_profiles
    ADD COLUMN IF NOT EXISTS relocation_ready BOOLEAN;

UPDATE candidate_questionnaire_profiles
SET relocation_ready = CASE relocation_readiness
                           WHEN 'READY' THEN TRUE
                           ELSE FALSE
    END
WHERE relocation_ready IS NULL;

ALTER TABLE candidate_questionnaire_profiles
    ALTER COLUMN relocation_ready SET NOT NULL;

ALTER TABLE candidate_questionnaire_profiles
DROP COLUMN IF EXISTS relocation_readiness;

UPDATE candidate_questionnaire_profiles
SET allow_related_experience_drafts =
        COALESCE(heuristic_skill_drafts_enabled, FALSE);

ALTER TABLE candidate_questionnaire_profiles
DROP COLUMN IF EXISTS heuristic_skill_drafts_enabled;

UPDATE candidate_questionnaire_profiles
SET additional_confirmed_facts = ''
WHERE additional_confirmed_facts IS NULL;

ALTER TABLE candidate_questionnaire_profiles
    ALTER COLUMN additional_confirmed_facts SET DEFAULT '';

ALTER TABLE candidate_questionnaire_profiles
    ALTER COLUMN additional_confirmed_facts SET NOT NULL;

ALTER TABLE candidate_questionnaire_profiles
DROP CONSTRAINT IF EXISTS chk_candidate_questionnaire_profile_salary;

ALTER TABLE candidate_questionnaire_profiles
    ADD CONSTRAINT chk_candidate_questionnaire_profiles_salary_range
        CHECK (salary_min >= 0 AND salary_max >= salary_min);