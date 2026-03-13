package com.example.githubaccessreportbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing a single user's access summary – the user's login and
 * all repositories they have access to along with permission details.
 */
public class UserAccessSummary {

    /** GitHub username. */
    @JsonProperty("username")
    private String username;

    /** Total number of repositories this user has access to. */
    @JsonProperty("repository_count")
    private int repositoryCount;

    /** List of repositories this user can access with permission details. */
    @JsonProperty("repositories")
    private List<RepoAccessEntry> repositories;

    public UserAccessSummary() {
    }

    public UserAccessSummary(String username, List<RepoAccessEntry> repositories) {
        this.username = username;
        this.repositoryCount = repositories.size();
        this.repositories = repositories;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the GitHub username.
     * @return username
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the number of repositories this user has access to.
     * @return repository count
     */
    public int getRepositoryCount() {
        return repositoryCount;
    }

    public void setRepositoryCount(int repositoryCount) {
        this.repositoryCount = repositoryCount;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the list of repository access entries for this user.
     * @return list of RepoAccessEntry
     */
    public List<RepoAccessEntry> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepoAccessEntry> repositories) {
        this.repositories = repositories;
    }
}
