package com.fairhire.storage;

import java.io.IOException;

/**
 * Storage service abstraction for persisted resume documents.
 * Allows transparent replacement of local filesystem storage with S3/GCS/MinIO object storage.
 */
public interface StorageService {

    /**
     * Store binary content associated with an uploaded document.
     *
     * @param content          Raw binary payload
     * @param originalFilename Original client-supplied filename (for extension extraction only)
     * @param sha256           SHA-256 integrity hash of the content
     * @return Storage key or relative identifier (never an absolute filesystem path)
     * @throws IOException If storage I/O fails
     */
    String store(byte[] content, String originalFilename, String sha256) throws IOException;

    /**
     * Load binary content by storage key.
     *
     * @param storageKey Storage key returned by {@link #store(byte[], String, String)}
     * @return Raw binary content
     * @throws IOException If retrieval fails or key is invalid
     */
    byte[] load(String storageKey) throws IOException;

    /**
     * Delete document by storage key.
     *
     * @param storageKey Storage key to delete
     * @throws IOException If deletion fails
     */
    void delete(String storageKey) throws IOException;

    /**
     * Check if a document exists for the given storage key.
     *
     * @param storageKey Storage key to check
     * @return true if document exists, false otherwise
     */
    boolean exists(String storageKey);
}
