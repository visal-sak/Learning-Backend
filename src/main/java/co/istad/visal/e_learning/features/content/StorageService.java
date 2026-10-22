package co.istad.visal.e_learning.features.content;

import java.time.Duration;

/**
 * Abstracts object storage. Swap the implementation for S3, MinIO or a local
 * folder without touching the access rules.
 */
public interface StorageService {

    /** A time-limited link. Never hand out the raw key. */
    String signedUrl(String storageKey, Duration ttl);

    String upload(byte[] bytes, String fileName, String contentType);

    void delete(String storageKey);
}
