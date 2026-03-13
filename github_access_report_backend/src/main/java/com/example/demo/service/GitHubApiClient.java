package com.example.githubaccessreportbackend.service;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.dto.CollaboratorInfo;
import com.example.githubaccessreportbackend.dto.RepositoryInfo;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Low-level GitHub REST API client responsible for making paginated HTTP calls
 * to retrieve organization repositories and repository collaborators.
 *
 * <p>This client handles:
 * <ul>
 *   <li>Automatic pagination via the GitHub Link header</li>
 *   <li>Error mapping to {@link GitHubApiException}</li>
 *   <li>Configurable page size</li>
 * </ul>
 */
@Service
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

    /** Pattern to extract the "next" page URL from the GitHub Link header. */
    private static final Pattern NEXT_LINK_PATTERN =
            Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final WebClient webClient;
    private final int perPage;

    /**
     * Constructs the GitHubApiClient with the configured WebClient and properties.
     *
     * @param gitHubWebClient the pre-configured WebClient for GitHub API calls
     * @param properties      the GitHub configuration properties
     */
    public GitHubApiClient(WebClient gitHubWebClient, GitHubProperties properties) {
        this.webClient = gitHubWebClient;
        this.perPage = properties.getApi().getPerPage();
    }

    // PUBLIC_INTERFACE
    /**
     * Fetches all repositories for the given GitHub organization, handling pagination.
     *
     * @param org the GitHub organization name
     * @return a complete list of repositories in the organization
     * @throws GitHubApiException if the GitHub API returns an error
     */
    public List<RepositoryInfo> fetchOrganizationRepos(String org) {
        log.info("Fetching repositories for organization: {}", org);
        String initialUrl = "/orgs/" + org + "/repos?per_page=" + perPage + "&type=all&page=1";
        List<RepositoryInfo> allRepos = fetchAllPages(initialUrl, new ParameterizedTypeReference<List<RepositoryInfo>>() {});
        log.info("Fetched {} repositories for organization '{}'", allRepos.size(), org);
        return allRepos;
    }

    // PUBLIC_INTERFACE
    /**
     * Fetches all collaborators for a given repository, handling pagination.
     * Uses the "affiliation=all" parameter to include all collaborator types.
     *
     * @param owner the repository owner (organization name)
     * @param repo  the repository name
     * @return a complete list of collaborators for the repository
     * @throws GitHubApiException if the GitHub API returns an error
     */
    public List<CollaboratorInfo> fetchRepoCollaborators(String owner, String repo) {
        log.debug("Fetching collaborators for repository: {}/{}", owner, repo);
        String initialUrl = "/repos/" + owner + "/" + repo + "/collaborators?per_page=" + perPage + "&affiliation=all&page=1";
        List<CollaboratorInfo> collaborators = fetchAllPages(initialUrl,
                new ParameterizedTypeReference<List<CollaboratorInfo>>() {});
        log.debug("Fetched {} collaborators for {}/{}", collaborators.size(), owner, repo);
        return collaborators;
    }

    /**
     * Generic paginated fetch that follows GitHub's Link header "next" URLs
     * until all pages have been retrieved.
     *
     * @param url      the initial URL (relative to base)
     * @param typeRef  the type reference for deserialization
     * @param <T>      the element type
     * @return aggregated list from all pages
     */
    private <T> List<T> fetchAllPages(String url, ParameterizedTypeReference<List<T>> typeRef) {
        List<T> allItems = new ArrayList<>();
        String currentUrl = url;

        while (currentUrl != null) {
            final String requestUrl = currentUrl;
            log.debug("Fetching page: {}", requestUrl);

            try {
                // Use exchange to access both body and headers
                PageResult<T> pageResult = webClient.get()
                        .uri(requestUrl)
                        .exchangeToMono(response -> {
                            HttpStatusCode statusCode = response.statusCode();
                            if (statusCode.isError()) {
                                return response.bodyToMono(String.class)
                                        .defaultIfEmpty("No response body")
                                        .flatMap(body -> Mono.error(
                                                new GitHubApiException(
                                                        "GitHub API error: " + body,
                                                        statusCode.value())));
                            }

                            // Extract Link header for pagination
                            String linkHeader = response.headers().asHttpHeaders().getFirst("Link");
                            String nextUrl = extractNextUrl(linkHeader);

                            return response.bodyToMono(typeRef)
                                    .map(items -> new PageResult<>(items, nextUrl));
                        })
                        .block();

                if (pageResult != null && pageResult.items != null) {
                    allItems.addAll(pageResult.items);
                    currentUrl = pageResult.nextUrl;
                } else {
                    currentUrl = null;
                }

            } catch (GitHubApiException e) {
                throw e;
            } catch (WebClientResponseException e) {
                throw new GitHubApiException(
                        "GitHub API request failed: " + e.getMessage(),
                        e.getStatusCode().value(), e);
            } catch (Exception e) {
                throw new GitHubApiException(
                        "Failed to fetch data from GitHub: " + e.getMessage(), 500, e);
            }
        }

        return allItems;
    }

    /**
     * Extracts the "next" page URL from a GitHub Link header.
     *
     * @param linkHeader the value of the Link header
     * @return the next page URL, or null if there is no next page
     */
    private String extractNextUrl(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Internal record-like class to hold a page of results plus the next URL.
     */
    private static class PageResult<T> {
        final List<T> items;
        final String nextUrl;

        PageResult(List<T> items, String nextUrl) {
            this.items = items;
            this.nextUrl = nextUrl;
        }
    }
}
