package com.example.githubaccessreportbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a GitHub repository as returned by the GitHub API.
 * Only the fields relevant to the access report are mapped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryInfo {

    /** Unique repository ID. */
    private long id;

    /** Full name of the repository (e.g., "org/repo-name"). */
    @JsonProperty("full_name")
    private String fullName;

    /** Short name of the repository. */
    private String name;

    /** Whether the repository is private. */
    @JsonProperty("private")
    private boolean isPrivate;

    /** URL for viewing the repository in a browser. */
    @JsonProperty("html_url")
    private String htmlUrl;

    public RepositoryInfo() {
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the unique repository ID.
     * @return repository ID
     */
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the full name of the repository (org/repo).
     * @return full repository name
     */
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the short name of the repository.
     * @return repository name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
}
