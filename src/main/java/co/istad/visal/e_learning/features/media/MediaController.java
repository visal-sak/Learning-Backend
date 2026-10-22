package co.istad.visal.e_learning.features.media;

import co.istad.visal.e_learning.features.media.dto.MediaResponse;
import co.istad.visal.e_learning.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Uploads are gated by role: only people who author content need them.
 * Reads are open, because thumbnails appear on the public catalog.
 */
@RestController
@RequestMapping("/api/v1/medias")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse upload(@RequestPart("file") MultipartFile file) {
        return mediaService.upload(file);
    }

    @PostMapping("/multiples")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MediaResponse> upload(@RequestPart("files") List<MultipartFile> files) {
        return mediaService.upload(files);
    }

    @GetMapping("/{name}")
    public MediaResponse findByName(@PathVariable String name) {
        return mediaService.findByName(name);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public PageResponse<MediaResponse> findAll(
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "25") int pageSize) {

        return mediaService.findAll(pageNumber, pageSize);
    }

    @PutMapping("/{name}/is-draft")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void draftByName(@PathVariable String name) {
        mediaService.draftByName(name);
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByName(@PathVariable String name) {
        mediaService.deleteByName(name);
    }
}