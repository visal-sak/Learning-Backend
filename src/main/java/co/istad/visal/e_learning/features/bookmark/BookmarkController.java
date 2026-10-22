package co.istad.visal.e_learning.features.bookmark;

import co.istad.visal.e_learning.features.bookmark.dto.BookmarkResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Under /my/** — a bookmark always belongs to the signed-in user. */
@RestController
@RequestMapping("/api/v1/my/bookmarks")
public class BookmarkController {

    private final BookmarkService service;

    public BookmarkController(BookmarkService service) {
        this.service = service;
    }

    @GetMapping
    public List<BookmarkResponse> mine() {
        return service.findMine();
    }

    @PutMapping("/{lessonId}")
    public BookmarkResponse add(@PathVariable UUID lessonId) {
        return service.add(lessonId);
    }

    @DeleteMapping("/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID lessonId) {
        service.remove(lessonId);
    }
}
