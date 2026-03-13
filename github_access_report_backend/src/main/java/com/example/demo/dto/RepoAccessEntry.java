package com.example.githubaccessreportbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a single repository access entry for a user in the access report.
 * Contains the repository name and the user's permission level.
 */
public class RepoAccessEntry {

    /** Full name of the repository (org/repo). */
    @JsonProperty("repository")
    private String repository;

    /** The user's permission level on this repository (e.g., admin, write, read). */
    @JsonProperty("permission")
    private String permission;

    /** Whether the repository is private. */
    @JsonProperty("private")
    private boolean isPrivate;

    public RepoAccessEntry() {
    }

    public RepoAccessEntry(String repository, String permission, boolean isPrivate) {
        this.repository = repository;
        this.permission = permission;
        this.isPrivate = isPrivate;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the full repository name.
     * @return repository name
     */
    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the permission level.
     * @return permission string
     */
    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
