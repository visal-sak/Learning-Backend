package co.istad.visal.e_learning.features.category;

import co.istad.visal.e_learning.features.category.dto.CategoryResponse;
import co.istad.visal.e_learning.features.category.dto.CreateCategoryRequest;
import co.istad.visal.e_learning.features.category.dto.UpdateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Categories are the one module decided by role alone — there is no ownership
 * question, because a category belongs to the platform rather than to a person.
 * That is why every write here carries a plain hasRole check and nothing more.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    // ---------- reads: open to everyone, including anonymous ----------

    @GetMapping
    public List<CategoryResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/slug/{slug}")
    public CategoryResponse getBySlug(@PathVariable String slug) {
        return service.findBySlug(slug);
    }

    // ---------- writes: ADMIN realm role required ----------

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request,
            UriComponentsBuilder uriBuilder) {

        CategoryResponse created = service.create(request);

        URI location = uriBuilder.path("/api/categories/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public CategoryResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
