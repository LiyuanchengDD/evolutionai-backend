package com.example.grpcdemo.storage;

/**
 * Exception thrown when interacting with Supabase object storage fails.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

