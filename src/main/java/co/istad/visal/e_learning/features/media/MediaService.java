package co.istad.visal.e_learning.features.media;

import co.istad.visal.e_learning.features.media.dto.MediaResponse;
import co.istad.visal.e_learning.utils.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    MediaResponse upload(MultipartFile file);

    List<MediaResponse> upload(List<MultipartFile> files);

    MediaResponse findByName(String name);

    PageResponse<MediaResponse> findAll(int pageNumber, int pageSize);

    /** First half of the two-step delete: mark it, then delete it. */
    void draftByName(String name);

    void deleteByName(String name);
}