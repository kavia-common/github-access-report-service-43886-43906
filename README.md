# GitHub Access Report Service

A Spring Boot backend service that connects to GitHub and generates a report showing which users have access to which repositories within a given organization. The service authenticates with GitHub, retrieves relevant data, and exposes a REST API endpoint that returns a structured access report in JSON format.

## Features

- **GitHub Authentication**: Secure authentication using a Personal Access Token (PAT)
- **Paginated API Calls**: Automatically handles GitHub API pagination for organizations with 100+ repositories
- **Concurrent Fetching**: Uses a configurable thread pool to fetch collaborators for multiple repositories concurrently, supporting 1000+ users efficiently
- **Aggregated User View**: Maps each user to all repositories they can access, with permission details (admin, write, read, etc.)
- **REST API**: Clean JSON endpoint for generating access reports
- **Error Handling**: Comprehensive error handling with structured JSON error responses
- **OpenAPI/Swagger Documentation**: Auto-generated API docs available via Swagger UI

## Tech Stack

- **Java 17**
- **Spring Boot 3.4.x**
- **Spring WebFlux (WebClient)** for non-blocking HTTP calls to GitHub API
- **Gradle** build system
- **SpringDoc OpenAPI** for API documentation

## Prerequisites

- **Java 17** or higher
- **Gradle 8.x** (included via Gradle Wrapper)
- A **GitHub Personal Access Token** (classic) with the following scopes:
  - `repo` – Full control of private repositories (needed to list collaborators)
  - `read:org` – Read organization membership

## How to Run

### 1. Clone the Repository

```bash
git clone <repository-url>
cd github-access-report-service-43886-43906/github_access_report_backend
```

### 2. Configure Authentication

Set the `GITHUB_TOKEN` environment variable with your GitHub Personal Access Token:

```bash
# Linux/macOS
export GITHUB_TOKEN=ghp_your_personal_access_token_here

# Windows (PowerShell)
$env:GITHUB_TOKEN = "ghp_your_personal_access_token_here"

# Windows (CMD)
set GITHUB_TOKEN=ghp_your_personal_access_token_here
```

**How to generate a token:**
1. Go to [GitHub Settings → Developer Settings → Personal Access Tokens (Classic)](https://github.com/settings/tokens)
2. Click "Generate new token (classic)"
3. Select scopes: `repo` and `read:org`
4. Copy the generated token and set it as described above

### 3. Run the Application

```bash
# Using Gradle Wrapper (recommended)
./gradlew bootRun

# Or build and run the JAR
./gradlew build
java -jar build/libs/githubaccessreportbackend-0.1.0.jar
```

The service will start on port **3001** (or configured port).

### 4. Optional Configuration

You can customize the service using environment variables:

| Variable | Default | Description |
|---|---|---|
| `GITHUB_TOKEN` | *(required)* | GitHub Personal Access Token |
| `GITHUB_API_BASE_URL` | `https://api.github.com` | GitHub API base URL (useful for GitHub Enterprise) |
| `GITHUB_API_MAX_CONCURRENCY` | `10` | Max concurrent requests to GitHub API |
| `GITHUB_API_PER_PAGE` | `100` | Items per page for pagination (max 100) |

## How to Call the API

### Generate Access Report

```
GET /api/v1/access-report?org={organization_name}
```

**Parameters:**
- `org` (required) – The GitHub organization name

**Example Request:**
```bash
curl "http://localhost:3001/api/v1/access-report?org=my-org"
```

**Example Response:**
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

### Other Endpoints

| Endpoint | Description |
|---|---|
| `GET /` | Welcome message |
| `GET /health` | Health check |
| `GET /docs` | Redirects to Swagger UI |
| `GET /swagger-ui.html` | Swagger UI (interactive API documentation) |
| `GET /api-docs` | OpenAPI JSON specification |
| `GET /api/info` | Application information |

## Architecture & Design Decisions

### Concurrent Fetching Strategy
The service uses `CompletableFuture` with a bounded thread pool (`ExecutorService`) to fetch collaborators for multiple repositories in parallel. This is critical for scalability — an organization with 100+ repos would take too long with sequential API calls.

### Pagination
All GitHub API calls (repos and collaborators) use automatic pagination by following the `Link` header's `rel="next"` URL. The page size is configurable (default: 100, which is the GitHub maximum).

### User-Centric Aggregation
The raw GitHub data is repository-centric (each repo has a list of collaborators). The service aggregates this into a user-centric view: each user maps to all repositories they have access to with permission details.

### Error Handling
- **401/403 from GitHub**: Returned as HTTP 401 to the client with details
- **404 from GitHub**: Returned as HTTP 404 (organization not found)
- **Missing token**: Returns HTTP 500 with a clear configuration error message
- **Timeout on collaborator fetch**: Logged and skipped (empty collaborator list for that repo)
- **403 on specific repos**: Logged and skipped (some repos restrict collaborator listing)

### No Database
This service is stateless and does not persist data. Each API call fetches fresh data from GitHub, ensuring the report always reflects the current state of access.

### WebClient over RestTemplate
The service uses Spring WebFlux's `WebClient` (instead of the deprecated `RestTemplate`) for HTTP calls to the GitHub API. WebClient provides better control over response handling including access to headers for pagination.

## Project Structure

```
src/main/java/com/example/demo/
├── githubaccessreportbackendApplication.java   # Main entry point
├── config/
│   ├── AsyncConfig.java                         # Thread pool configuration
│   ├── GitHubClientConfig.java                  # WebClient bean setup
│   ├── GitHubProperties.java                    # Configuration properties
│   └── OpenApiConfig.java                       # Swagger/OpenAPI metadata
├── controller/
│   └── AccessReportController.java              # REST endpoint
├── dto/
│   ├── AccessReport.java                        # Full report response DTO
│   ├── CollaboratorInfo.java                     # GitHub collaborator DTO
│   ├── RepoAccessEntry.java                      # Single repo access entry DTO
│   ├── RepositoryInfo.java                       # GitHub repository DTO
│   └── UserAccessSummary.java                    # User access summary DTO
├── exception/
│   ├── ConfigurationException.java               # Config error exception
│   ├── GitHubApiException.java                   # GitHub API error exception
│   └── GlobalExceptionHandler.java               # Centralized error handling
└── service/
    ├── AccessReportService.java                  # Report generation orchestrator
    └── GitHubApiClient.java                      # GitHub API client with pagination
```
