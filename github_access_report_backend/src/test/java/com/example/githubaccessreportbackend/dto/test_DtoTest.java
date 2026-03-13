package com.example.githubaccessreportbackend.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DTO classes.
 *
 * <p>Covers:
 * <ul>
 *   <li>CollaboratorInfo.Permissions#toPermissionLevel() priority resolution</li>
 *   <li>AccessReport constructor and field initialization</li>
 *   <li>UserAccessSummary constructor and field initialization</li>
 *   <li>RepoAccessEntry constructor and field initialization</li>
 *   <li>RepositoryInfo getters and setters</li>
 * </ul>
 */
class test_DtoTest {

    // --- Permissions.toPermissionLevel Tests ---

    @Nested
    @DisplayName("CollaboratorInfo.Permissions.toPermissionLevel()")
    class PermissionsTests {

        @Test
        @DisplayName("Should return 'admin' when admin is true")
        void shouldReturnAdmin() {
            CollaboratorInfo.Permissions perms = new CollaboratorInfo.Permissions();
            perms.setAdmin(true);
            perms.setMaintain(true);
            perms.setPush(true);
            perms.setTriage(true);
            perms.setPull(true);

            assertThat(perms.toPermissionLevel()).isEqualTo("admin");
        }

        @Test
        @DisplayName("Should return 'maintain' when maintain is highest")
        void shouldReturnMaintain() {
            CollaboratorInfo.Permissions perms = new CollaboratorInfo.Permissions();
            perms.setAdmin(false);
            perms.setMaintain(true);
            perms.setPush(true);
            perms.setTriage(true);
            perms.setPull(true);

            assertThat(perms.toPermissionLevel()).isEqualTo("maintain");
        }

        @Test
        @DisplayName("Should return 'write' when push is highest")
        void shouldReturnWrite() {
            CollaboratorInfo.Permissions perms = new CollaboratorInfo.Permissions();
            perms.setAdmin(false);
            perms.setMaintain(false);
            perms.setPush(true);
            perms.setTriage(false);
            perms.setPull(true);

            assertThat(perms.toPermissionLevel()).isEqualTo("write");
        }

        @Test
        @DisplayName("Should return 'triage' when triage is highest")
        void shouldReturnTriage() {
            CollaboratorInfo.Permissions perms = new CollaboratorInfo.Permissions();
            perms.setAdmin(false);
            perms.setMaintain(false);
            perms.setPush(false);
            perms.setTriage(true);
            perms.setPull(true);

            assertThat(perms.toPermissionLevel()).isEqualTo("triage");
        }

        @Test
        @DisplayName("Should return 'read' when only pull is true")
        void shouldReturnRead() {
            CollaboratorInfo.Permissions perms = new CollaboratorInfo.Permissions();
            perms.setAdmin(false);
            perms.setMaintain(false);
            perms.setPush(false);
            perms.setTriage(false);
            perms.setPull(true);

            assertThat(perms.toPermissionLevel()).isEqualTo("read");
        }

        @Test
        @DisplayName("Should return 'none' when no permissions are set")
        void shouldReturnNone() {
            CollaboratorInfo.Permissions perms = new CollaboratorInfo.Permissions();
            perms.setAdmin(false);
            perms.setMaintain(false);
            perms.setPush(false);
            perms.setTriage(false);
            perms.setPull(false);

            assertThat(perms.toPermissionLevel()).isEqualTo("none");
        }
    }

    // --- AccessReport Tests ---

    @Nested
    @DisplayName("AccessReport")
    class AccessReportTests {

        @Test
        @DisplayName("Constructor should set all fields correctly")
        void constructorShouldSetFields() {
            RepoAccessEntry entry = new RepoAccessEntry("org/repo1", "admin", true);
            UserAccessSummary user = new UserAccessSummary("alice", List.of(entry));
            AccessReport report = new AccessReport("test-org", 5, List.of(user));

            assertThat(report.getOrganization()).isEqualTo("test-org");
            assertThat(report.getTotalRepositories()).isEqualTo(5);
            assertThat(report.getTotalUsers()).isEqualTo(1);
            assertThat(report.getUsers()).hasSize(1);
            assertThat(report.getGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("Constructor should auto-calculate totalUsers from user list size")
        void shouldAutoCalculateTotalUsers() {
            UserAccessSummary u1 = new UserAccessSummary("alice", List.of());
            UserAccessSummary u2 = new UserAccessSummary("bob", List.of());
            AccessReport report = new AccessReport("org", 3, List.of(u1, u2));

            assertThat(report.getTotalUsers()).isEqualTo(2);
        }

        @Test
        @DisplayName("Default constructor should create empty AccessReport")
        void defaultConstructorShouldWork() {
            AccessReport report = new AccessReport();
            assertThat(report.getOrganization()).isNull();
            assertThat(report.getGeneratedAt()).isNull();
            assertThat(report.getUsers()).isNull();
        }
    }

    // --- UserAccessSummary Tests ---

    @Nested
    @DisplayName("UserAccessSummary")
    class UserAccessSummaryTests {

        @Test
        @DisplayName("Constructor should set username and calculate repository count")
        void constructorShouldSetFields() {
            RepoAccessEntry e1 = new RepoAccessEntry("org/repo1", "admin", false);
            RepoAccessEntry e2 = new RepoAccessEntry("org/repo2", "read", true);
            UserAccessSummary summary = new UserAccessSummary("alice", List.of(e1, e2));

            assertThat(summary.getUsername()).isEqualTo("alice");
            assertThat(summary.getRepositoryCount()).isEqualTo(2);
            assertThat(summary.getRepositories()).hasSize(2);
        }

        @Test
        @DisplayName("Constructor should handle empty repositories list")
        void constructorWithEmptyRepos() {
            UserAccessSummary summary = new UserAccessSummary("bob", List.of());

            assertThat(summary.getUsername()).isEqualTo("bob");
            assertThat(summary.getRepositoryCount()).isEqualTo(0);
            assertThat(summary.getRepositories()).isEmpty();
        }
    }

    // --- RepoAccessEntry Tests ---

    @Nested
    @DisplayName("RepoAccessEntry")
    class RepoAccessEntryTests {

        @Test
        @DisplayName("Constructor should set all fields")
        void constructorShouldSetFields() {
            RepoAccessEntry entry = new RepoAccessEntry("org/repo1", "write", true);

            assertThat(entry.getRepository()).isEqualTo("org/repo1");
            assertThat(entry.getPermission()).isEqualTo("write");
            assertThat(entry.isPrivate()).isTrue();
        }

        @Test
        @DisplayName("Setters should update fields")
        void settersShouldWork() {
            RepoAccessEntry entry = new RepoAccessEntry();
            entry.setRepository("org/repo2");
            entry.setPermission("admin");
            entry.setPrivate(false);

            assertThat(entry.getRepository()).isEqualTo("org/repo2");
            assertThat(entry.getPermission()).isEqualTo("admin");
            assertThat(entry.isPrivate()).isFalse();
        }
    }

    // --- RepositoryInfo Tests ---

    @Nested
    @DisplayName("RepositoryInfo")
    class RepositoryInfoTests {

        @Test
        @DisplayName("Should support all getters and setters")
        void shouldSupportGettersAndSetters() {
            RepositoryInfo repo = new RepositoryInfo();
            repo.setId(42);
            repo.setFullName("org/repo");
            repo.setName("repo");
            repo.setPrivate(true);
            repo.setHtmlUrl("https://github.com/org/repo");

            assertThat(repo.getId()).isEqualTo(42);
            assertThat(repo.getFullName()).isEqualTo("org/repo");
            assertThat(repo.getName()).isEqualTo("repo");
            assertThat(repo.isPrivate()).isTrue();
            assertThat(repo.getHtmlUrl()).isEqualTo("https://github.com/org/repo");
        }
    }

    // --- CollaboratorInfo Tests ---

    @Nested
    @DisplayName("CollaboratorInfo")
    class CollaboratorInfoTests {

        @Test
        @DisplayName("Should support all getters and setters")
        void shouldSupportGettersAndSetters() {
            CollaboratorInfo collab = new CollaboratorInfo();
            collab.setId(123);
            collab.setLogin("alice");
            collab.setAvatarUrl("https://avatar.example.com/alice");
            collab.setRoleName("admin");

            assertThat(collab.getId()).isEqualTo(123);
            assertThat(collab.getLogin()).isEqualTo("alice");
            assertThat(collab.getAvatarUrl()).isEqualTo("https://avatar.example.com/alice");
            assertThat(collab.getRoleName()).isEqualTo("admin");
        }
    }
}
