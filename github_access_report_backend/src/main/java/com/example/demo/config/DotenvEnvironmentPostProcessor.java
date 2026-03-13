package com.example.githubaccessreportbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link EnvironmentPostProcessor} that reads a {@code .env} file from the
 * application's working directory and adds its key-value pairs as a property source
 * to the Spring {@link ConfigurableEnvironment}.
 *
 * <p>This ensures that variables defined in the {@code .env} file (such as
 * {@code GITHUB_TOKEN}) are available for property placeholder resolution
 * (e.g., {@code ${GITHUB_TOKEN}}) in {@code application.properties}, even
 * when the variables are not exported as OS-level environment variables.</p>
 *
 * <p>The property source has lower precedence than actual OS environment
 * variables and system properties, so real environment variables always win.</p>
 *
 * <p>Supported .env file format:
 * <ul>
 *   <li>Lines with {@code KEY=VALUE} pairs</li>
 *   <li>Lines starting with {@code #} are comments and are ignored</li>
 *   <li>Empty lines are ignored</li>
 *   <li>Values may optionally be quoted with single or double quotes</li>
 * </ul>
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    // PUBLIC_INTERFACE
    /**
     * Reads the {@code .env} file (if present) from the working directory and
     * registers its entries as a low-priority property source in the Spring
     * Environment.
     *
     * @param environment the Spring configurable environment
     * @param application the Spring application being bootstrapped
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        FileSystemResource dotenvFile = new FileSystemResource(".env");
        if (!dotenvFile.exists()) {
            log.debug(".env file not found in working directory; skipping dotenv loading");
            return;
        }

        Map<String, Object> dotenvProperties = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dotenvFile.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    log.debug(".env line {} skipped (no valid KEY=VALUE format): {}", lineNumber, trimmed);
                    continue;
                }

                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();

                // Remove optional surrounding quotes (single or double)
                value = stripQuotes(value);

                // Only add if not already set by a real OS environment variable
                // This ensures OS env vars take precedence
                if (environment.getProperty(key) == null) {
                    dotenvProperties.put(key, value);
                }
            }

        } catch (IOException e) {
            log.warn("Failed to read .env file: {}", e.getMessage());
            return;
        }

        if (!dotenvProperties.isEmpty()) {
            // Add with lowest priority (last) so that system properties, OS env vars,
            // and application.properties all take precedence where they define the same key
            environment.getPropertySources()
                    .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvProperties));
            log.info("Loaded {} properties from .env file", dotenvProperties.size());
        }
    }

    /**
     * Strips optional surrounding single or double quotes from a value string.
     *
     * @param value the raw value
     * @return the value with surrounding quotes removed, if present
     */
    private String stripQuotes(String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
