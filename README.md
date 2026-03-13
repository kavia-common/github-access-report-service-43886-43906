# GitHub Access Report Service

A Spring Boot backend service that connects to GitHub and generates a report showing which users have access to which repositories within a given organization. The service authenticates with GitHub using a Personal Access Token, retrieves repository and collaborator data via the GitHub REST API, aggregates this information into a user-centric view, and exposes a REST API endpoint that returns a structured access report in JSON format.

## Features

- **Secure GitHub Authentication**: Authenticates with GitHub using a Personal Access Token (PAT) passed via environment variable or `.env` file, ensuring credentials are never hard-coded.
- **Paginated API Calls**: Automatically handles GitHub API pagination by following the `Link` header, supporting organizations with 100+ repositories seamlessly.
- **Concurrent Collaborator Fetching**: Uses a bounded thread pool (`ExecutorService`) to fetch collaborators for multiple repositories in parallel, efficiently handling organizations with 1000+ users.
- **User-Centric Aggregation**: Transforms the raw repository-centric data from GitHub into an aggregated view that maps each user to all repositories they can access, including permission details (admin, write, read).
- **Structured JSON API**: Exposes a clean REST endpoint returning a comprehensive access report in JSON format.
- **Comprehensive Error Handling**: Centralised `@RestControllerAdvice` exception handler produces structured JSON error responses for all failure modes (authentication errors, missing parameters, organisation not found, etc.).
- **OpenAPI / Swagger Documentation**: Auto-generated interactive API documentation available via Swagger UI.

## Tech Stack

- **Java 17**
- **Spring Boot 3.4.x**
- **Spring WebFlux (WebClient)** for non-blocking HTTP calls to the GitHub API
- **Gradle 8.x** build system (included via Gradle Wrapper)
- **SpringDoc OpenAPI 2.8.x** for API documentation
- **JUnit 5 + MockWebServer** for testing

## Prerequisites

- **Java 17** or higher installed on your machine
- **Gradle 8.x** (the Gradle Wrapper is included, so no separate installation is required)
- A **GitHub Personal Access Token (classic)** with the following scopes:
  - `repo` – Full control of private repositories (required to list collaborators on private repos)
  - `read:org` – Read organization membership

## How to Run the Project

### 1. Clone the Repository

```bash
git clone <repository-url>
cd github-access-report-service-43886-43906/github_access_report_backend
```

### 2. Configure Authentication

The service requires a GitHub Personal Access Token (PAT) for authentication. You can provide it via either an environment variable or a `.env` file.

#### Option A: Environment Variable

Set the `GITHUB_TOKEN` environment variable directly:

```bash
# Linux / macOS
export GITHUB_TOKEN=ghp_your_personal_access_token_here

# Windows (PowerShell)
$env:GITHUB_TOKEN = "ghp_your_personal_access_token_here"

# Windows (CMD)
set GITHUB_TOKEN=ghp_your_personal_access_token_here
```

#### Option B: `.env` File

Create a `.env` file in the `github_access_report_backend/` directory (the working directory where you run the application):

```
# .env
GITHUB_TOKEN=ghp_your_personal_access_token_here
```

The application includes a custom `DotenvEnvironmentPostProcessor` that automatically reads the `.env` file at startup and loads its variables into the Spring environment. OS-level environment variables take precedence over values defined in the `.env` file, so you can safely use both approaches.

#### How to Generate a GitHub Personal Access Token

1. Go to [GitHub Settings → Developer Settings → Personal Access Tokens (Classic)](https://github.com/settings/tokens).
2. Click **"Generate new token (classic)"**.
3. Give the token a descriptive name (e.g., "Access Report Service").
4. Select the required scopes: **`repo`** and **`read:org`**.
5. Click **"Generate token"** and copy the token value.
6. Configure the token as described above (environment variable or `.env` file).

### 3. Build and Run the Application

```bash
# Using the Gradle Wrapper (recommended)
./gradlew bootRun

# Or build the JAR first, then run it
./gradlew build
java -jar build/libs/githubaccessreportbackend-0.1.0.jar
```

The service starts on port **3001** by default. You can override the port by setting the `PORT` environment variable.

### 4. Optional Configuration

The following environment variables can be used to customise the service behaviour:

| Variable | Default | Description |
|---|---|---|
| `GITHUB_TOKEN` | *(required)* | GitHub Personal Access Token for authentication |
| `GITHUB_API_BASE_URL` | `https://api.github.com` | GitHub API base URL (useful for GitHub Enterprise Server) |
| `GITHUB_API_MAX_CONCURRENCY` | `10` | Maximum number of concurrent requests to the GitHub API |
| `GITHUB_API_PER_PAGE` | `100` | Items per page for GitHub API pagination (maximum 100) |
| `PORT` | `3001` | HTTP port the service listens on |

## How to Call the API Endpoint

### Generate an Access Report

Send a GET request to the access report endpoint, specifying the GitHub organisation name as a query parameter:

```
GET /api/v1/access-report?org={organization_name}
```

**Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `org` | query string | Yes | The GitHub organisation name to generate the report for |

**Example using `curl`:**

```bash
curl "http://localhost:3001/api/v1/access-report?org=my-org"
```

**Example Success Response (HTTP 200):**

```json
{
  "organization": "my-org",
  "generated_at": "2024-01-15T10:30:00.123Z",
  "total_repositories": 25,
  "total_users": 12,
  "users": [
    {
      "username": "alice",
      "repository_count": 5,
      "repositories": [
        {
          "repository": "my-org/api-service",
          "permission": "admin",
          "private": true
        },
        {
          "repository": "my-org/frontend-app",
          "permission": "write",
          "private": false
        }
      ]
    },
    {
      "username": "bob",
      "repository_count": 3,
      "repositories": [
        {
          "repository": "my-org/api-service",
          "permission": "read",
          "private": true
        }
      ]
    }
  ]
}
```

The response includes the organisation name, a timestamp of when the report was generated, the total counts of repositories and unique users, and an array of user access summaries. Each user summary contains the username, the number of repositories they have access to, and a list of repository entries with the repository name, permission level, and visibility.

**Example Error Response (HTTP 401 – Invalid Token):**

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "GitHub API error: Bad credentials"
}
```

### Other Available Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `GET /` | GET | Welcome message |
| `GET /health` | GET | Health check (returns `OK`) |
| `GET /docs` | GET | Redirects to Swagger UI |
| `GET /swagger-ui.html` | GET | Swagger UI – interactive API documentation |
| `GET /api-docs` | GET | Redirects to OpenAPI JSON specification |
| `GET /api/info` | GET | Basic application information |

## Assumptions

The following assumptions were made during the design and implementation of this service:

1. **Authentication via Personal Access Token (PAT):** The assignment requires "secure authentication" with GitHub. A classic Personal Access Token was chosen as the authentication mechanism because it is the most straightforward approach for server-side applications that need to access organisation-level data. The token is provided exclusively through environment variables or a `.env` file, ensuring it is never committed to source control.

2. **Organisation-level access only:** The service assumes the target organisation is accessible by the authenticated user. If the user does not have the required permissions on the organisation, the GitHub API will return appropriate errors (401 or 404) which are propagated to the caller.

3. **Collaborator-based access model:** The service retrieves access information using the GitHub collaborators API (`/repos/{owner}/{repo}/collaborators`). This captures users who have been explicitly granted access to each repository. It does not capture implicit access through GitHub team-level permissions unless those users are also listed as collaborators.

4. **Stateless design (no database):** The service does not persist any data. Each API call fetches fresh information directly from GitHub, ensuring the report always reflects the current state of access. This was chosen because access permissions change frequently and a cached report could quickly become stale.

5. **Rate limits are the caller's responsibility:** The service makes authenticated API calls which benefit from GitHub's higher rate limit (5,000 requests/hour). However, for very large organisations, a single report request may consume a significant portion of this budget. The service does not implement rate-limit backoff or retry logic; it is assumed the caller schedules report generation at appropriate intervals.

6. **Some repositories may restrict collaborator listing:** Certain repositories (e.g., forks or repositories requiring push access to list collaborators) may return a 403 when fetching collaborators. The service logs a warning and returns an empty collaborator list for those repositories rather than failing the entire report.

7. **Permission resolution:** The service resolves the highest permission level from the permissions object returned by GitHub (admin > maintain > write > triage > read). If only a `role_name` field is available instead of detailed permissions, that value is used directly.

8. **Concurrent fetching with bounded parallelism:** To meet the scale requirement of 100+ repositories and 1000+ users, the service uses a configurable fixed thread pool (default: 10 threads) to fetch repository collaborators concurrently. This avoids overwhelming the GitHub API while still achieving significant speedup over sequential calls.

## Design Decisions

### Architecture Overview

The application follows a standard layered architecture with clear separation of concerns:

```
Controller → Service → API Client → GitHub REST API
```

- **Controller layer** (`AccessReportController`) handles HTTP request/response mapping and input validation.
- **Service layer** (`AccessReportService`) orchestrates the report generation workflow: validation, data fetching, and aggregation.
- **API Client layer** (`GitHubApiClient`) is a low-level HTTP client responsible for paginated calls to the GitHub REST API.

### WebClient over RestTemplate

The service uses Spring WebFlux's `WebClient` instead of the deprecated `RestTemplate` for making HTTP calls to the GitHub API. `WebClient` provides better control over response handling, including access to HTTP headers (needed for pagination) and a modern, reactive API.

### Concurrent Fetching Strategy

The service uses `CompletableFuture` with a bounded `ExecutorService` thread pool to fetch collaborators for all repositories in parallel. Each repository's collaborator list is fetched as a separate asynchronous task. The thread pool size is configurable (default: 10) to balance throughput against GitHub API rate limits. A 60-second timeout is applied per repository to prevent a single slow response from blocking the entire report.

### Automatic Pagination

All GitHub API calls (for both repositories and collaborators) implement automatic pagination by following the `Link` header's `rel="next"` URL. The page size is configurable (default: 100, which is the GitHub API maximum). This ensures the service can handle organisations of any size without manual page management.

### User-Centric Aggregation

The raw data from GitHub is repository-centric: each repository endpoint returns a list of collaborators. The service inverts this into a user-centric view using an in-memory `TreeMap`, so each user maps to all repositories they have access to along with their permission level. The result is sorted alphabetically by username for consistent output.

### Structured Error Handling

A centralised `GlobalExceptionHandler` (using `@RestControllerAdvice`) catches all exceptions and converts them into consistent JSON error responses with `timestamp`, `status`, `error`, and `message` fields. Specific exception types are mapped to appropriate HTTP status codes:

- `GitHubApiException` with status 401/403 → HTTP 401 (Unauthorized)
- `GitHubApiException` with status 404 → HTTP 404 (Not Found)
- `GitHubApiException` with other statuses → HTTP 502 (Bad Gateway)
- `ConfigurationException` → HTTP 500 (missing token)
- `MissingServletRequestParameterException` → HTTP 400 (Bad Request)

### No Database

The service is intentionally stateless. There is no database dependency, and JPA/DataSource auto-configuration is explicitly excluded. This simplifies deployment and ensures the report always reflects the live state of GitHub permissions.

## Project Structure

```
src/main/java/com/example/demo/
├── githubaccessreportbackendApplication.java   # Main Spring Boot entry point
├── HelloController.java                         # Welcome, health, docs endpoints
├── config/
│   ├── AsyncConfig.java                         # Thread pool for concurrent fetching
│   ├── DotenvEnvironmentPostProcessor.java      # Loads .env file at startup
│   ├── GitHubClientConfig.java                  # WebClient bean with auth headers
│   ├── GitHubProperties.java                    # @ConfigurationProperties for github.*
│   └── OpenApiConfig.java                       # Swagger/OpenAPI metadata
├── controller/
│   └── AccessReportController.java              # REST endpoint: GET /api/v1/access-report
├── dto/
│   ├── AccessReport.java                        # Full report response DTO
│   ├── CollaboratorInfo.java                    # GitHub collaborator response DTO
│   ├── RepoAccessEntry.java                     # Single repo access entry for a user
│   ├── RepositoryInfo.java                      # GitHub repository response DTO
│   └── UserAccessSummary.java                   # Per-user access summary DTO
├── exception/
│   ├── ConfigurationException.java              # Thrown when config is invalid
│   ├── GitHubApiException.java                  # Thrown when GitHub API returns an error
│   └── GlobalExceptionHandler.java              # Centralised JSON error responses
└── service/
    ├── AccessReportService.java                 # Report generation orchestrator
    └── GitHubApiClient.java                     # Paginated GitHub API client
```

## Running Tests

The project includes unit tests for all major components. To run the test suite:

```bash
./gradlew test
```

Test classes are located under `src/test/java/com/example/githubaccessreportbackend/` and cover:

- **`AccessReportControllerTest`** – Tests the REST endpoint, parameter validation, and error responses.
- **`AccessReportServiceTest`** – Tests the report generation logic, concurrent fetching, and aggregation.
- **`GitHubApiClientTest`** – Tests paginated API calls using MockWebServer.
- **`DtoTest`** – Tests DTO serialisation/deserialisation and field mapping.
- **`GlobalExceptionHandlerTest`** – Tests that exceptions produce correct HTTP status codes and JSON error bodies.
