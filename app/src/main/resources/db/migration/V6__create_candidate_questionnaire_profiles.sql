CREATE TABLE candidate_questionnaire_profiles (
    user_id UUID PRIMARY KEY,
    timezone_id VARCHAR(64) NOT NULL,
    salary_min INTEGER NOT NULL,
    salary_max INTEGER NOT NULL,
    salary_currency VARCHAR(3) NOT NULL,
    relocation_readiness VARCHAR(32) NOT NULL,
    work_format_preference VARCHAR(32) NOT NULL,
    remote_work_priority BOOLEAN NOT NULL DEFAULT FALSE,
    english_level VARCHAR(32) NOT NULL,
    business_trips_ready BOOLEAN NOT NULL DEFAULT FALSE,
    start_availability VARCHAR(32) NOT NULL,
    heuristic_skill_drafts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    additional_confirmed_facts TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_candidate_questionnaire_profiles_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_candidate_questionnaire_profile_salary
        CHECK (salary_min > 0 AND salary_max >= salary_min),

    CONSTRAINT chk_candidate_questionnaire_profile_currency
        CHECK (salary_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_candidate_questionnaire_profile_relocation
        CHECK (relocation_readiness IN ('READY', 'NOT_READY')),

    CONSTRAINT chk_candidate_questionnaire_profile_work_format
        CHECK (work_format_preference IN ('ANY', 'REMOTE', 'HYBRID', 'OFFICE')),

    CONSTRAINT chk_candidate_questionnaire_profile_start_availability
        CHECK (
            start_availability IN (
                'IMMEDIATELY',
                'WITHIN_TWO_WEEKS',
                'WITHIN_ONE_MONTH',
                'NEGOTIABLE'
            )
        )
);
