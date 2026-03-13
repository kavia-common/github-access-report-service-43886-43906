package com.example.githubaccessreportbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * DTO representing the complete access report for a GitHub organization.
 * Contains metadata about the report and the aggregated user-to-repository access mappings.
 */
public class AccessReport {

    /** The GitHub organization name this report was generated for. */
    @JsonProperty("organization")
    private String organization;

    /** ISO-8601 timestamp of when the report was generated. */
    @JsonProperty("generated_at")
    private String generatedAt;

    /** Total number of repositories in the organization. */
    @JsonProperty("total_repositories")
    private int totalRepositories;

    /** Total number of unique users with access. */
    @JsonProperty("total_users")
    private int totalUsers;

    /** Aggregated user access summaries. */
    @JsonProperty("users")
    private List<UserAccessSummary> users;

    public AccessReport() {
    }

    public AccessReport(String organization, int totalRepositories, List<UserAccessSummary> users) {
        this.organization = organization;
        this.generatedAt = Instant.now().toString();
        this.totalRepositories = totalRepositories;
        this.totalUsers = users.size();
        this.users = users;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the organization name.
     * @return organization name
     */
    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the timestamp when the report was generated.
     * @return ISO-8601 formatted timestamp
     */
    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the total number of repositories in the organization.
     * @return total repository count
     */
    public int getTotalRepositories() {
        return totalRepositories;
    }

    public void setTotalRepositories(int totalRepositories) {
        this.totalRepositories = totalRepositories;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the total number of unique users with access.
     * @return total user count
     */
    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the list of user access summaries.
     * @return list of UserAccessSummary
     */
    public List<UserAccessSummary> getUsers() {
        return users;
    }

    public void setUsers(List<UserAccessSummary> users) {
        this.users = users;
    }
}
