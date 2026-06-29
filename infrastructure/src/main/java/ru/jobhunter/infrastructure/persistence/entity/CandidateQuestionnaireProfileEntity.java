package ru.jobhunter.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.jobhunter.core.domain.model.CandidateSalaryTaxBasis;
import ru.jobhunter.core.domain.model.CandidateStartAvailability;
import ru.jobhunter.core.domain.model.CandidateTestAssignmentReadiness;
import ru.jobhunter.core.domain.model.CandidateWorkFormatPreference;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_questionnaire_profiles")
public class CandidateQuestionnaireProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "time_zone_id", nullable = false, length = 64)
    private String timeZoneId;

    @Column(name = "salary_min", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", nullable = false, length = 3)
    private String salaryCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_tax_basis", nullable = false, length = 20)
    private CandidateSalaryTaxBasis salaryTaxBasis;

    @Column(name = "relocation_ready", nullable = false)
    private boolean relocationReady;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_format_preference", nullable = false, length = 20)
    private CandidateWorkFormatPreference workFormatPreference;

    @Column(name = "remote_work_priority", nullable = false)
    private boolean remoteWorkPriority;

    @Column(name = "english_level", nullable = false, length = 32)
    private String englishLevel;

    @Column(name = "business_trips_ready", nullable = false)
    private boolean businessTripsReady;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "test_assignment_readiness",
            nullable = false,
            length = 16
    )
    private CandidateTestAssignmentReadiness testAssignmentReadiness;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_availability", nullable = false, length = 30)
    private CandidateStartAvailability startAvailability;

    @Column(name = "allow_related_experience_drafts", nullable = false)
    private boolean allowRelatedExperienceDrafts;

    @Column(
            name = "additional_confirmed_facts",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String additionalConfirmedFacts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CandidateQuestionnaireProfileEntity() {
    }

    public CandidateTestAssignmentReadiness getTestAssignmentReadiness() {
        return testAssignmentReadiness;
    }

    public void setTestAssignmentReadiness(CandidateTestAssignmentReadiness testAssignmentReadiness) {
        this.testAssignmentReadiness = testAssignmentReadiness;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public void setSalaryCurrency(String salaryCurrency) {
        this.salaryCurrency = salaryCurrency;
    }

    public CandidateSalaryTaxBasis getSalaryTaxBasis() {
        return salaryTaxBasis;
    }

    public void setSalaryTaxBasis(
            CandidateSalaryTaxBasis salaryTaxBasis
    ) {
        this.salaryTaxBasis = salaryTaxBasis;
    }

    public boolean isRelocationReady() {
        return relocationReady;
    }

    public void setRelocationReady(boolean relocationReady) {
        this.relocationReady = relocationReady;
    }

    public CandidateWorkFormatPreference getWorkFormatPreference() {
        return workFormatPreference;
    }

    public void setWorkFormatPreference(
            CandidateWorkFormatPreference workFormatPreference
    ) {
        this.workFormatPreference = workFormatPreference;
    }

    public boolean isRemoteWorkPriority() {
        return remoteWorkPriority;
    }

    public void setRemoteWorkPriority(boolean remoteWorkPriority) {
        this.remoteWorkPriority = remoteWorkPriority;
    }

    public String getEnglishLevel() {
        return englishLevel;
    }

    public void setEnglishLevel(String englishLevel) {
        this.englishLevel = englishLevel;
    }

    public boolean isBusinessTripsReady() {
        return businessTripsReady;
    }

    public void setBusinessTripsReady(boolean businessTripsReady) {
        this.businessTripsReady = businessTripsReady;
    }

    public CandidateStartAvailability getStartAvailability() {
        return startAvailability;
    }

    public void setStartAvailability(
            CandidateStartAvailability startAvailability
    ) {
        this.startAvailability = startAvailability;
    }

    public boolean isAllowRelatedExperienceDrafts() {
        return allowRelatedExperienceDrafts;
    }

    public void setAllowRelatedExperienceDrafts(
            boolean allowRelatedExperienceDrafts
    ) {
        this.allowRelatedExperienceDrafts = allowRelatedExperienceDrafts;
    }

    public String getAdditionalConfirmedFacts() {
        return additionalConfirmedFacts;
    }

    public void setAdditionalConfirmedFacts(
            String additionalConfirmedFacts
    ) {
        this.additionalConfirmedFacts = additionalConfirmedFacts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}