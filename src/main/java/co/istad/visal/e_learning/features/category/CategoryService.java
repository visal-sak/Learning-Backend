package co.istad.visal.e_learning.features.category;

import co.istad.visal.e_learning.features.category.dto.CategoryResponse;
import co.istad.visal.e_learning.features.category.dto.CreateCategoryRequest;
import co.istad.visal.e_learning.features.category.dto.UpdateCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    /** Open to everyone, including anonymous visitors. */
    List<CategoryResponse> findAll();

    CategoryResponse findById(UUID id);

    CategoryResponse findBySlug(String slug);

    /** Admin only — enforced at the controller. */
    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(UUID id, UpdateCategoryRequest request);

    /** Rejected while any course still references the category. */
    void delete(UUID id);
}
