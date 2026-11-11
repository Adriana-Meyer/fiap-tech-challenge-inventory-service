package com.fiap.workshop.inventory.domain.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, Object identifier) {
        super(resourceName + " already exists: " + identifier);
    }
}
