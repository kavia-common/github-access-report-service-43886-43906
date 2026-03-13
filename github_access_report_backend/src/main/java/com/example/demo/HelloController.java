package com.example.githubaccessreportbackend;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Basic application endpoints including welcome, health check, documentation redirects,
 * and application information.
 */
@RestController
@Tag(name = "Hello Controller", description = "Basic endpoints for githubaccessreportbackend")
public class HelloController {

    // PUBLIC_INTERFACE
    /**
     * Returns a welcome message for the root endpoint.
     *
     * @return welcome message string
     */
    @GetMapping("/")
    @Operation(summary = "Welcome endpoint", description = "Returns a welcome message")
    public String hello() {
        return "Hello, Spring Boot! Welcome to githubaccessreportbackend";
    }

    // PUBLIC_INTERFACE
    /**
     * Redirects to Swagger UI, preserving original scheme/host/port via X-Forwarded-* headers.
     *
     * @param request the incoming HTTP request
     * @return redirect to Swagger UI
     */
    @GetMapping("/docs")
    @Operation(summary = "API Documentation", description = "Redirects to Swagger UI preserving original scheme/host/port")
    public RedirectView docs(HttpServletRequest request) {
        // Build an absolute URL based on the incoming request, honoring X-Forwarded-* headers
        String target = UriComponentsBuilder
                .fromHttpRequest(new ServletServerHttpRequest(request))
                .replacePath("/swagger-ui.html")
                .replaceQuery(null)
                .build()
                .toUriString();

        RedirectView rv = new RedirectView(target);
        // Use HTTP 1.1 compatible redirects when necessary (preserves 303/307 semantics if used)
        rv.setHttp10Compatible(false);
        return rv;
    }

    // PUBLIC_INTERFACE
    /**
     * Backward-compatible redirect from /api-docs to the actual OpenAPI JSON spec at /v3/api-docs.
     *
     * @param request the incoming HTTP request
     * @return redirect to the OpenAPI JSON endpoint
     */
    @Hidden
    @GetMapping("/api-docs")
    public RedirectView apiDocs(HttpServletRequest request) {
        String target = UriComponentsBuilder
                .fromHttpRequest(new ServletServerHttpRequest(request))
                .replacePath("/v3/api-docs")
                .replaceQuery(null)
                .build()
                .toUriString();

        RedirectView rv = new RedirectView(target);
        rv.setHttp10Compatible(false);
        return rv;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns application health status.
     *
     * @return "OK" string
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns application health status")
    public String health() {
        return "OK";
    }

    // PUBLIC_INTERFACE
    /**
     * Returns basic application information.
     *
     * @return application info string
     */
    @GetMapping("/api/info")
    @Operation(summary = "Application info", description = "Returns application information")
    public String info() {
        return "Spring Boot Application: githubaccessreportbackend";
    }
} 