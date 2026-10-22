package co.istad.visal.e_learning.features.media;

import co.istad.visal.e_learning.features.media.dto.MediaResponse;
import co.istad.visal.e_learning.security.AuthUtils;
import co.istad.visal.e_learning.utils.PageResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final String MB = "MB";

    /**
     * Whitelist, not blacklist. Without this an instructor can upload a .html
     * or .svg and, because the files are served from your own origin, run
     * script in your users' browsers.
     */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final MediaRepository mediaRepository;

    @Value("${media.location}")
    private String mediaLocation;

    @Value("${media.client-path}")
    private String mediaClientPath;

    @Value("${media.base-uri}")
    private String mediaBaseUri;

    /**
     * Files.copy fails if the directory is absent, and a missing trailing
     * slash silently writes D:/mediauuid.png instead of D:/media/uuid.png.
     * Both are fixed here rather than left to whoever edits the properties.
     */
    @PostConstruct
    void prepareStorage() {
        if (!mediaLocation.endsWith("/") && !mediaLocation.endsWith("\\")) {
            mediaLocation = mediaLocation + "/";
        }

        try {
            Files.createDirectories(Paths.get(mediaLocation));
            log.info("Media directory ready at {}", Paths.get(mediaLocation).toAbsolutePath());
        } catch (IOException ex) {
            log.error("Could not create media directory {}", mediaLocation, ex);
        }
    }

    @Transactional
    @Override
    public MediaResponse upload(MultipartFile file) {
        validate(file);

        String name = UUID.randomUUID().toString();
        String extension = extensionOf(file.getOriginalFilename());
        Path path = Paths.get(buildMediaPath(name, extension));

        log.info("Uploading media to {}", path);

        try {
            Files.copy(file.getInputStream(), path);
        } catch (IOException ex) {
            log.error("Media upload failed writing to {}", path, ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Media upload failed: " + ex.getMessage());
        }

        Media media = new Media();
        media.setName(name);
        media.setExtension(extension);
        media.setSize((float) file.getSize());
        media.setMediaType(file.getContentType());
        media.setIsDraft(false);
        media.setUploadedBy(AuthUtils.currentUserId().orElse(null));

        return buildMediaResponse(mediaRepository.save(media));
    }

    @Override
    public List<MediaResponse> upload(List<MultipartFile> files) {
        return files.stream().map(this::upload).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResponse findByName(String name) {
        return buildMediaResponse(load(name));
    }

    /**
     * Returns PageResponse rather than Page. Serializing a PageImpl straight
     * to JSON works but Spring warns about it, because the shape is an
     * implementation detail that can change between versions.
     */
    @Transactional(readOnly = true)
    @Override
    public PageResponse<MediaResponse> findAll(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return PageResponse.from(
                mediaRepository.findByIsDraft(Boolean.FALSE, pageable),
                this::buildMediaResponse);
    }

    @Transactional
    @Override
    public void draftByName(String name) {
        Media media = load(name);
        media.setIsDraft(Boolean.TRUE);
        mediaRepository.save(media);
    }

    @Transactional
    @Override
    public void deleteByName(String name) {
        Media media = load(name);

        // Returning silently here would look like success while the file
        // stayed put. Say why instead.
        if (!media.getIsDraft()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mark this media as draft before deleting it");
        }

        mediaRepository.delete(media);

        Path path = Paths.get(buildMediaPath(media.getName(), media.getExtension()));

        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.error("Could not delete {}", path, ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete the file");
        }
    }

    // ---------------------------------------------------------------

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file received");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Images must be 5 MB or smaller");
        }

        String type = file.getContentType();

        // Some clients send application/octet-stream when they cannot detect
        // the type, so fall back to the file extension before rejecting.
        boolean typeOk = type != null && ALLOWED_TYPES.contains(type.toLowerCase());
        boolean extensionOk = Set.of("jpg", "jpeg", "png", "webp", "gif")
                .contains(extensionOf(file.getOriginalFilename()));

        if (!typeOk && !extensionOk) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG, WebP and GIF images are accepted (received " + type + ")");
        }
    }

    /**
     * A filename can be null, and a file with no dot has no extension —
     * substring(lastIndexOf('.') + 1) throws on one and returns the whole
     * name on the other.
     */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "bin";
        }

        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return "bin";
        }

        return originalFilename.substring(lastDot + 1).toLowerCase();
    }

    private Media load(String name) {
        return mediaRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Media has not been found"));
    }

    private MediaResponse buildMediaResponse(Media media) {
        Float sizeAsMb = media.getSize() / 1_000_000;

        return MediaResponse.builder()
                .id(media.getId())
                .name(media.getName())
                .extension(media.getExtension())
                .mediaType(media.getMediaType())
                .size(sizeAsMb)
                .measurement(MB)
                .uri(buildMediaUri(media))
                .build();
    }

    private String buildMediaUri(Media media) {
        return mediaBaseUri + mediaClientPath + "/" + media.getName() + "." + media.getExtension();
    }

    private String buildMediaPath(String mediaName, String mediaExtension) {
        return mediaLocation + mediaName + "." + mediaExtension;
    }
}