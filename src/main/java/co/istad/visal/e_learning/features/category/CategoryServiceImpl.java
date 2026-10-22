package co.istad.visal.e_learning.features.category;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.features.category.dto.CategoryResponse;
import co.istad.visal.e_learning.features.category.dto.CreateCategoryRequest;
import co.istad.visal.e_learning.features.category.dto.UpdateCategoryRequest;
import co.istad.visal.e_learning.utils.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {


    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryServiceImpl(CategoryRepository repository, CategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream()
                .map(this::withCount)
                .toList();
    }

    @Override
    public CategoryResponse findById(UUID id) {
        return withCount(load(id));
    }

    @Override
    public CategoryResponse findBySlug(String slug) {
        Category category = repository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No category with slug " + slug));
        return withCount(category);
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = request.name().trim();

        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category named " + name + " already exists");
        }

        Category category = new Category(name, SlugUtils.unique(name, "category", repository::existsBySlug), request.description());
        return withCount(repository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = load(id);
        String name = request.name().trim();

        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A category named " + name + " already exists");
        }

        mapper.updateEntity(request, category);
        category.setName(name);

        // The slug is part of public URLs. Regenerating it on every rename would
        // break links that already exist, so it stays put once created.

        return withCount(category);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = load(id);
        long courseCount = repository.countCourses(id);

        if (courseCount > 0) {
            throw new ConflictException(
                    "Cannot delete this category: " + courseCount + " course(s) still use it. "
                            + "Move them to another category first.");
        }

        repository.delete(category);
    }

    private Category load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No category with id " + id));
    }

    private CategoryResponse withCount(Category category) {
        return mapper.toResponse(category, repository.countCourses(category.getId()));
    }

}
