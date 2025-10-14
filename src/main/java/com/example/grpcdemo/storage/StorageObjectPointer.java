package com.example.grpcdemo.storage;

import java.util.Objects;

/**
 * Immutable reference to an object stored in Supabase.
 */
public record StorageObjectPointer(String bucket,
                                   String path,
                                   Long sizeBytes,
                                   String contentType) {

    public StorageObjectPointer {
        Objects.requireNonNull(bucket, "bucket");
        Objects.requireNonNull(path, "path");
    }

    public boolean isEmpty() {
        return bucket.isBlank() || path.isBlank();
    }
}

