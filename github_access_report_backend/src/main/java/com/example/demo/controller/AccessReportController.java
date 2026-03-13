package com.example.githubaccessreportbackend.controller;

import com.example.githubaccessreportbackend.dto.AccessReport;
import com.example.githubaccessreportbackend.service.AccessReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the GitHub Access Report API.
 *
 * <p>Provides an endpoint that generates a report showing which users have access
 * to which repositories within a specified GitHub organization.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Access Report", description = "Endpoints for generating GitHub organization access reports")
public class AccessReportController {

    private static final Logger log = LoggerFactory.getLogger(AccessReportController.class);

    private final AccessReportService accessReportService;

    /**
     * Constructs the AccessReportController.
     *
     * @param accessReportService the service that generates access reports
     */
    public AccessReportController(AccessReportService accessReportService) {
        this.accessReportService = accessReportService;
    }

    // PUBLIC_INTERFACE
    /**
     * Generates and returns an access report for the specified GitHub organization.
     *
     * <p>The report maps each user to the repositories they can access within the
     * organization, along with their permission level (admin, write, read, etc.).
     *
     * <p>Example: {@code GET /api/v1/access-report?org=my-org}
     *
     * @param org the GitHub organization name (required)
     * @return ResponseEntity containing the structured AccessReport in JSON format
     */
    @GetMapping(value = "/access-report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate access report",
            description = "Generates a report mapping users to the repositories they have access to "
                    + "within the specified GitHub organization. Includes permission details for each "
                    + "user-repository pair. Supports organizations with 100+ repositories and 1000+ users."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Access report generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccessReport.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing required 'org' query parameter",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "401",
                    description = "GitHub authentication failed – invalid or missing token",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found on GitHub",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or configuration issue",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(
                    responseCode = "502",
                    description = "GitHub API returned an unexpected error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<AccessReport> getAccessReport(
            @Parameter(description = "GitHub organization name", required = true, example = "spring-projects")
            @RequestParam("org") String org) {

        log.info("Access report requested for organization: {}", org);

        // Validate org parameter
        String sanitizedOrg = org.trim();
        if (sanitizedOrg.isEmpty()) {
            throw new IllegalArgumentException("Organization name must not be empty");
        }

        AccessReport report = accessReportService.generateReport(sanitizedOrg);
        return ResponseEntity.ok(report);
    }
}
