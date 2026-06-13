CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(320) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE resumes (
                         id UUID PRIMARY KEY,
                         user_id UUID NOT NULL,
                         title VARCHAR(255) NOT NULL,
                         source_type VARCHAR(50) NOT NULL,
                         original_file_name VARCHAR(255),
                         content TEXT NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                         CONSTRAINT fk_resumes_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users (id)
                                 ON DELETE CASCADE
);

CREATE INDEX idx_resumes_user_id
    ON resumes (user_id);

CREATE TABLE vacancies (
                           id UUID PRIMARY KEY,
                           external_id VARCHAR(255),
                           platform VARCHAR(50) NOT NULL,
                           title VARCHAR(255) NOT NULL,
                           company_name VARCHAR(255),
                           url TEXT NOT NULL,
                           city VARCHAR(255),
                           salary_from NUMERIC(12, 2),
                           salary_to NUMERIC(12, 2),
                           currency VARCHAR(10),
                           employment_type VARCHAR(100),
                           description TEXT,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                           CONSTRAINT uq_vacancies_platform_external_id
                               UNIQUE (platform, external_id)
);

CREATE INDEX idx_vacancies_platform
    ON vacancies (platform);

CREATE INDEX idx_vacancies_title
    ON vacancies (title);

CREATE TABLE job_applications (
                                  id UUID PRIMARY KEY,
                                  user_id UUID NOT NULL,
                                  vacancy_id UUID NOT NULL,
                                  resume_id UUID,
                                  status VARCHAR(50) NOT NULL,
                                  cover_letter_text TEXT,
                                  applied_at TIMESTAMP WITH TIME ZONE,
                                  viewed_at TIMESTAMP WITH TIME ZONE,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                  CONSTRAINT fk_job_applications_user
                                      FOREIGN KEY (user_id)
                                          REFERENCES users (id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_job_applications_vacancy
                                      FOREIGN KEY (vacancy_id)
                                          REFERENCES vacancies (id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_job_applications_resume
                                      FOREIGN KEY (resume_id)
                                          REFERENCES resumes (id)
                                          ON DELETE SET NULL,

                                  CONSTRAINT uq_job_applications_user_vacancy
                                      UNIQUE (user_id, vacancy_id)
);

CREATE INDEX idx_job_applications_user_id
    ON job_applications (user_id);

CREATE INDEX idx_job_applications_vacancy_id
    ON job_applications (vacancy_id);

CREATE INDEX idx_job_applications_status
    ON job_applications (status);

CREATE TABLE hr_contacts (
                             id UUID PRIMARY KEY,
                             full_name VARCHAR(255),
                             company_name VARCHAR(255),
                             position_title VARCHAR(255),
                             email VARCHAR(320),
                             telegram_username VARCHAR(255),
                             vk_profile_url TEXT,
                             source_url TEXT,
                             source_type VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT uq_hr_contacts_email
                                 UNIQUE (email)
);

CREATE INDEX idx_hr_contacts_company_name
    ON hr_contacts (company_name);

CREATE INDEX idx_hr_contacts_source_type
    ON hr_contacts (source_type);