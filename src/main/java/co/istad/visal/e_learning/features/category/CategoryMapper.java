package co.istad.visal.e_learning.features.category;

import co.istad.visal.e_learning.features.category.dto.CategoryResponse;
import co.istad.visal.e_learning.features.category.dto.CreateCategoryRequest;
import co.istad.visal.e_learning.features.category.dto.UpdateCategoryRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "courseCount", source = "courseCount")
    CategoryResponse toResponse(Category category, long courseCount);

    /**
     * The slug is not mapped from the request — the service derives it from the
     * name and sets it explicitly. Leaving it unmapped here is deliberate.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateCategoryRequest request, @MappingTarget Category category);
}
