package com.example.learning_spring_security.Service.ServiceHandler;

import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.Repository.CategoryRepository;
import com.example.learning_spring_security.Repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service Handler for common validation and business logic checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceHandler {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    /**
     * Check if category name already exists
     * @param name category name
     * @throws CustomMessageException if category name already exists
     */
    public void validateCategoryNameNotExists(String name) {
        if (!StringUtils.hasText(name)) {
            throw new CustomMessageException(
                    "Category name cannot be empty",
                    String.valueOf(HttpStatus.BAD_REQUEST.value())
            );
        }

        if (categoryRepository.existsByName(name)) {
            throw new CustomMessageException(
                    "Category with name '" + name + "' already exists",
                    String.valueOf(HttpStatus.CONFLICT.value())
            );
        }
        log.info("Category name '{}' validation passed - name is unique", name);
    }

    /**
     * Check if category name is valid (not empty)
     * @param name category name
     * @throws CustomMessageException if category name is empty
     */
    public void validateCategoryName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new CustomMessageException(
                    "Category name cannot be empty or null",
                    String.valueOf(HttpStatus.BAD_REQUEST.value())
            );
        }
        log.info("Category name '{}' is valid", name);
    }

    /**
     * Check if subcategory name already exists for a given category
     * @param name subcategory name
     * @param categoryId category ID
     * @throws CustomMessageException if subcategory name already exists for the category
     */
    public void validateSubCategoryNameNotExists(String name, Long categoryId) {
        if (!StringUtils.hasText(name)) {
            throw new CustomMessageException(
                    "Subcategory name cannot be empty",
                    String.valueOf(HttpStatus.BAD_REQUEST.value())
            );
        }

        if (categoryId == null || categoryId <= 0) {
            throw new CustomMessageException(
                    "Invalid category ID",
                    String.valueOf(HttpStatus.BAD_REQUEST.value())
            );
        }

        if (subCategoryRepository.existsByNameAndCategoryId(name, categoryId)) {
            throw new CustomMessageException(
                    "Subcategory with name '" + name + "' already exists in this category",
                    String.valueOf(HttpStatus.CONFLICT.value())
            );
        }
        log.info("Subcategory name '{}' validation passed for category ID {}", name, categoryId);
    }

    /**
     * Check if subcategory name is valid (not empty)
     * @param name subcategory name
     * @throws CustomMessageException if subcategory name is empty
     */
    public void validateSubCategoryName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new CustomMessageException(
                    "Subcategory name cannot be empty or null",
                    String.valueOf(HttpStatus.BAD_REQUEST.value())
            );
        }
        log.info("Subcategory name '{}' is valid", name);
    }

    /**
     * Check if a string value exists (not empty, not null, not whitespace)
     * @param value string value to check
     * @param fieldName name of the field being validated
     * @throws CustomMessageException if value is empty
     */
    public void validateStringField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new CustomMessageException(
                    fieldName + " cannot be empty or null",
                    String.valueOf(HttpStatus.BAD_REQUEST.value())
            );
        }
        log.info("Field '{}' validation passed", fieldName);
    }

    /**
     * Check if ID is valid (not null, greater than 0)
     * @param id ID value to check
     * @param fieldName name of the ID field
     * @throws CustomMessageException if ID is invalid
     */
    public void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new CustomMessageException(
                    fieldName + " must be a valid positive number",
                    String.valueOf(HttpStatus.BAD_REQUEST.value())
            );
        }
        log.info("ID validation passed for {}: {}", fieldName, id);
    }

    /**
     * Generic validation for non-empty string with field name
     * @param value value to validate
     * @param fieldName name of the field
     * @return true if valid
     */
    public boolean isValidString(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            log.warn("Validation failed for field '{}': value is empty", fieldName);
            return false;
        }
        log.info("Field '{}' is valid", fieldName);
        return true;
    }

    /**
     * Generic validation for valid ID
     * @param id ID to validate
     * @return true if valid (not null and greater than 0)
     */
    public boolean isValidId(Long id) {
        return id != null && id > 0;
    }
}
