package co.istad.visal.e_learning.features.content;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Disk-backed storage for development.
 *
 * The signed URL is the part worth keeping when you move to S3 or MinIO: a
 * link that expires means a shared URL is useless by the time it is pasted,
 * which is what keeps the enrollment check from being decorative.
 */
@Service
public class LocalStorageService implements StorageService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Path root;
    private final String publicBaseUrl;
    private final byte[] signingKey;

    public LocalStorageService(
            @Value("${app.storage.local-path:./storage}") String localPath,
            @Value("${app.storage.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${app.storage.signing-key}") String signingKey) {

        this.root = Paths.get(localPath).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        this.signingKey = signingKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostConstruct
    void createRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create storage directory " + root, ex);
        }
    }

    @Override
    public String upload(byte[] bytes, String fileName, String contentType) {
        String key = UUID.randomUUID() + "-" + sanitize(fileName);
        Path target = resolve(key);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store " + fileName, ex);
        }

        return key;
    }

    @Override
    public String signedUrl(String storageKey, Duration ttl) {
        if (storageKey == null) {
            return null;
        }

        // Already an absolute URL (an external video host, say) — hand it back.
        if (storageKey.startsWith("http://") || storageKey.startsWith("https://")) {
            return storageKey;
        }

        long expiresAt = Instant.now().plus(ttl).getEpochSecond();
        String signature = sign(storageKey + ":" + expiresAt);

        return publicBaseUrl + "/api/files/" + storageKey + "?expires=" + expiresAt + "&signature=" + signature;
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not delete " + storageKey, ex);
        }
    }

    // ---------------------------------------------------------------
    //  Used by FileDownloadController
    // ---------------------------------------------------------------

    public boolean isSignatureValid(String storageKey, long expiresAt, String signature) {
        if (Instant.now().getEpochSecond() > expiresAt) {
            return false;
        }
        String expected = sign(storageKey + ":" + expiresAt);

        // Constant-time compare: a fast-failing equals leaks the signature
        // one character at a time to anyone willing to measure.
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    public Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();

        // Blocks ../../etc/passwd style keys.
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Storage key escapes the root directory");
        }
        return target;
    }

    // ---------------------------------------------------------------

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Could not sign the storage URL", ex);
        }
    }

    private String sanitize(String fileName) {
        return fileName == null ? "file" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}