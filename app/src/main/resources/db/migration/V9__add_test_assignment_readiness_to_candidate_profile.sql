ALTER TABLE candidate_questionnaire_profiles
    ADD COLUMN IF NOT EXISTS test_assignment_readiness
    VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE candidate_questionnaire_profiles
DROP CONSTRAINT IF EXISTS
    chk_candidate_questionnaire_profiles_test_assignment_readiness;

ALTER TABLE candidate_questionnaire_profiles
    ADD CONSTRAINT
        chk_candidate_questionnaire_profiles_test_assignment_readiness
        CHECK (
            test_assignment_readiness IN (
                                          'YES',
                                          'NO',
                                          'UNKNOWN'
                )
            );