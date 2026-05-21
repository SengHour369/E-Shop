package com.example.learning_spring_security.Service.ServiceHandler;

import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.Repository.CategoryRepository;
import com.example.learning_spring_security.Repository.SubCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceHandlerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @InjectMocks
    private ServiceHandler serviceHandler;

    @Test
    void validateCategoryNameNotExists_ShouldPass_WhenNameIsUnique() {
        // Given
        when(categoryRepository.existsByNameAndDeletedFalse("UniqueName")).thenReturn(false);

        // When & Then
        serviceHandler.validateCategoryNameNotExists("UniqueName");
        verify(categoryRepository).existsByNameAndDeletedFalse("UniqueName");
    }

    @Test
    void validateCategoryNameNotExists_ShouldThrowException_WhenNameIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateCategoryNameNotExists(""))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Category name cannot be empty");
    }

    @Test
    void validateCategoryNameNotExists_ShouldThrowException_WhenNameAlreadyExists() {
        // Given
        when(categoryRepository.existsByNameAndDeletedFalse("ExistingName")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateCategoryNameNotExists("ExistingName"))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Category with name 'ExistingName' already exists");
    }

    @Test
    void validateCategoryName_ShouldPass_WhenNameIsValid() {
        // When & Then
        serviceHandler.validateCategoryName("ValidName");
    }

    @Test
    void validateCategoryName_ShouldThrowException_WhenNameIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateCategoryName(""))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Category name cannot be empty or null");
    }

    @Test
    void validateSubCategoryNameNotExists_ShouldPass_WhenNameIsUnique() {
        // Given
        when(subCategoryRepository.existsByNameAndCategoryId("UniqueName", 1L)).thenReturn(false);

        // When & Then
        serviceHandler.validateSubCategoryNameNotExists("UniqueName", 1L);
        verify(subCategoryRepository).existsByNameAndCategoryId("UniqueName", 1L);
    }

    @Test
    void validateSubCategoryNameNotExists_ShouldThrowException_WhenNameIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateSubCategoryNameNotExists("", 1L))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Subcategory name cannot be empty");
    }

    @Test
    void validateSubCategoryNameNotExists_ShouldThrowException_WhenCategoryIdIsInvalid() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateSubCategoryNameNotExists("Name", null))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Invalid category ID");
    }

    @Test
    void validateSubCategoryNameNotExists_ShouldThrowException_WhenNameAlreadyExists() {
        // Given
        when(subCategoryRepository.existsByNameAndCategoryId("ExistingName", 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateSubCategoryNameNotExists("ExistingName", 1L))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Subcategory with name 'ExistingName' already exists in this category");
    }

    @Test
    void validateSubCategoryName_ShouldPass_WhenNameIsValid() {
        // When & Then
        serviceHandler.validateSubCategoryName("ValidName");
    }

    @Test
    void validateSubCategoryName_ShouldThrowException_WhenNameIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateSubCategoryName(""))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Subcategory name cannot be empty or null");
    }

    @Test
    void validateStringField_ShouldPass_WhenValueIsValid() {
        // When & Then
        serviceHandler.validateStringField("ValidValue", "TestField");
    }

    @Test
    void validateStringField_ShouldThrowException_WhenValueIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateStringField("", "TestField"))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("TestField cannot be empty or null");
    }

    @Test
    void validateId_ShouldPass_WhenIdIsValid() {
        // When & Then
        serviceHandler.validateId(1L, "TestId");
    }

    @Test
    void validateId_ShouldThrowException_WhenIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateId(null, "TestId"))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("TestId must be a valid positive number");
    }

    @Test
    void validateId_ShouldThrowException_WhenIdIsZero() {
        // When & Then
        assertThatThrownBy(() -> serviceHandler.validateId(0L, "TestId"))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("TestId must be a valid positive number");
    }

    @Test
    void isValidString_ShouldReturnTrue_WhenValueIsValid() {
        // When
        boolean result = serviceHandler.isValidString("ValidValue", "TestField");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isValidString_ShouldReturnFalse_WhenValueIsEmpty() {
        // When
        boolean result = serviceHandler.isValidString("", "TestField");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValidId_ShouldReturnTrue_WhenIdIsValid() {
        // When
        boolean result = serviceHandler.isValidId(1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isValidId_ShouldReturnFalse_WhenIdIsNull() {
        // When
        boolean result = serviceHandler.isValidId(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValidId_ShouldReturnFalse_WhenIdIsZero() {
        // When
        boolean result = serviceHandler.isValidId(0L);

        // Then
        assertThat(result).isFalse();
    }
}
