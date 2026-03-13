package com.example.githubaccessreportbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI / Swagger metadata for the service.
 */
@Configuration
public class OpenApiConfig {

    // PUBLIC_INTERFACE
    /**
     * Creates the OpenAPI specification metadata for the GitHub Access Report service.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GitHub Access Report API")
                        .version("1.0.0")
                        .description(
                                "A service that connects to GitHub and generates a report showing which "
                                        + "users have access to which repositories within a given organization. "
                                        + "Supports concurrent fetching, pagination, and aggregated user-to-repo mapping.")
                        .contact(new Contact()
                                .name("GitHub Access Report Team")));
    }
}
