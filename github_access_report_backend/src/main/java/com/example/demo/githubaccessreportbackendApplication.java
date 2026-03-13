package com.example.githubaccessreportbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main entry point for the GitHub Access Report backend service.
 *
 * <p>This Spring Boot application connects to the GitHub API, retrieves
 * organization repository and collaborator data, and exposes a REST endpoint
 * that returns a structured access report in JSON format.
 *
 * <p>JPA/DataSource auto-configuration is excluded since this service
 * does not use a database.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ConfigurationPropertiesScan("com.example.githubaccessreportbackend.config")
public class githubaccessreportbackendApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(githubaccessreportbackendApplication.class, args);
    }
}
