package com.example.githubaccessreportbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a GitHub collaborator on a repository, as returned by the GitHub API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollaboratorInfo {

    /** GitHub user ID. */
    private long id;

    /** GitHub username (login). */
    private String login;

    /** Avatar URL. */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** Role name (e.g., admin, write, read). */
    @JsonProperty("role_name")
    private String roleName;

    /** Permissions object from GitHub API. */
    private Permissions permissions;

    public CollaboratorInfo() {
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the GitHub user ID.
     * @return user ID
     */
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the GitHub username.
     * @return login name
     */
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    /**
     * Nested class representing the permissions a collaborator has on a repository.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Permissions {
        private boolean admin;
        private boolean maintain;
        private boolean push;
        private boolean triage;
        private boolean pull;

        public boolean isAdmin() {
            return admin;
        }

        public void setAdmin(boolean admin) {
            this.admin = admin;
        }

        public boolean isMaintain() {
            return maintain;
        }

        public void setMaintain(boolean maintain) {
            this.maintain = maintain;
        }

        public boolean isPush() {
            return push;
        }

        public void setPush(boolean push) {
            this.push = push;
        }

        public boolean isTriage() {
            return triage;
        }

        public void setTriage(boolean triage) {
            this.triage = triage;
        }

        public boolean isPull() {
            return pull;
        }

        public void setPull(boolean pull) {
            this.pull = pull;
        }

        /**
         * Derives the highest permission level as a human-readable string.
         * @return permission level string
         */
        public String toPermissionLevel() {
            if (admin) return "admin";
            if (maintain) return "maintain";
            if (push) return "write";
            if (triage) return "triage";
            if (pull) return "read";
            return "none";
        }
    }
}
