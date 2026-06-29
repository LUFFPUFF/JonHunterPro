package ru.jobhunter.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auto_response_queue_items")
public class AutoResponseQueueItemEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "external_vacancy_id", nullable = false, length = 100)
    private String externalVacancyId;

    @Column(name = "vacancy_name", nullable = false)
    private String vacancyName;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "vacancy_url")
    private String vacancyUrl;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "candidate_approval_reason")
    private String candidateApprovalReason;

    @Column(name = "diagnostic_directory")
    private String diagnosticDirectory;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AutoResponseQueueItemEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalVacancyId() {
        return externalVacancyId;
    }

    public void setExternalVacancyId(String externalVacancyId) {
        this.externalVacancyId = externalVacancyId;
    }

    public String getVacancyName() {
        return vacancyName;
    }

    public void setVacancyName(String vacancyName) {
        this.vacancyName = vacancyName;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getVacancyUrl() {
        return vacancyUrl;
    }

    public void setVacancyUrl(String vacancyUrl) {
        this.vacancyUrl = vacancyUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getCandidateApprovalReason() {
        return candidateApprovalReason;
    }

    public void setCandidateApprovalReason(
            String candidateApprovalReason
    ) {
        this.candidateApprovalReason = candidateApprovalReason;
    }

    public String getDiagnosticDirectory() {
        return diagnosticDirectory;
    }

    public void setDiagnosticDirectory(
            String diagnosticDirectory
    ) {
        this.diagnosticDirectory = diagnosticDirectory;
    }
}
