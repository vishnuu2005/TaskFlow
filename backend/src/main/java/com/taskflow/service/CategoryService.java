package com.taskflow.service;

import com.taskflow.dto.CategoryDto;
import com.taskflow.entity.Category;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.repository.CategoryRepository;
import com.taskflow.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        requireAdmin();
        if (categoryRepository.existsByName(dto.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists: " + dto.name());
        }

        Category cat = new Category(
                dto.name().trim(),
                dto.description() != null ? dto.description().trim() : null,
                dto.color() != null ? dto.color().trim() : "#8b5cf6"
        );
        Category saved = categoryRepository.save(cat);
        return toDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        requireAdmin();
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (!cat.getName().equalsIgnoreCase(dto.name().trim())) {
            if (categoryRepository.existsByName(dto.name().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists: " + dto.name());
            }
            cat.setName(dto.name().trim());
        }

        cat.setDescription(dto.description());
        if (dto.color() != null) {
            cat.setColor(dto.color());
        }

        Category saved = categoryRepository.save(cat);
        return toDto(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        requireAdmin();
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        categoryRepository.delete(cat);
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getColor());
    }

    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can manage categories");
        }
    }
}
