package com.example.githubaccessreportbackend.exception;

/**
 * Custom exception thrown when the application configuration is invalid or incomplete.
 */
public class ConfigurationException extends RuntimeException {

    // PUBLIC_INTERFACE
    /**
     * Constructs a new ConfigurationException.
     *
     * @param message description of the configuration issue
     */
    public ConfigurationException(String message) {
        super(message);
    }
}
