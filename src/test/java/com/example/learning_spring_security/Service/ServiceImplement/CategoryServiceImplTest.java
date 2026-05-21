package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Category;
import com.example.learning_spring_security.Repository.CategoryRepository;
import com.example.learning_spring_security.dto.Request.CategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryRequest categoryRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .build();
        categoryRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build();
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void createCategory_ShouldCreateSuccessfully() {
        // Given
        when(categoryRepository.existsByNameAndDeletedFalse("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        ResponseErrorTemplate response = categoryService.createCategory(categoryRequest);

        // Then
        assertThat(response).isNotNull();
        verify(categoryRepository).existsByNameAndDeletedFalse("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_ShouldThrowException_WhenCategoryExists() {
        // Given
        when(categoryRepository.existsByNameAndDeletedFalse("Electronics")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(categoryRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category already exists with name: Electronics");
    }

    @Test
    void getCategoryById_ShouldReturnCategory_WhenExists() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // When
        ResponseErrorTemplate response = categoryService.getCategoryById(1L);

        // Then
        assertThat(response).isNotNull();
        verify(categoryRepository).findById(1L);
    }

    @Test
    void getCategoryById_ShouldThrowException_WhenNotFound() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.getCategoryById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");
    }

    @Test
    void getCategoryByName_ShouldReturnCategory_WhenExists() {
        // Given
        when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(category));

        // When
        ResponseErrorTemplate response = categoryService.getCategoryByName("Electronics");

        // Then
        assertThat(response).isNotNull();
        verify(categoryRepository).findByName("Electronics");
    }

    @Test
    void getCategoryByName_ShouldThrowException_WhenNotFound() {
        // Given
        when(categoryRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.getCategoryByName("NonExistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with name: NonExistent");
    }

    @Test
    void getAllCategories_ShouldReturnPagedCategories() {
        // Given
        Page<Category> categoryPage = new PageImpl<>(List.of(category));
        when(categoryRepository.findAll(pageable)).thenReturn(categoryPage);

        // When
        Page<ResponseErrorTemplate> response = categoryService.getAllCategories(pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(categoryRepository).findAll(pageable);
    }

    @Test
    void getAllCategories_ShouldReturnListOfCategories() {
        // Given
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        // When
        List<ResponseErrorTemplate> response = categoryService.getAllCategories();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        verify(categoryRepository).findAll();
    }

    @Test
    void updateCategory_ShouldUpdateSuccessfully() {
        // Given
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Updated Electronics")
                .description("Updated description")
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndDeletedFalse("Updated Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        ResponseErrorTemplate response = categoryService.updateCategory(1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_ShouldThrowException_WhenCategoryNotFound() {
        // Given
        CategoryRequest updateRequest = CategoryRequest.builder().build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");
    }

    @Test
    void updateCategory_ShouldThrowException_WhenNewNameExists() {
        // Given
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Existing Name")
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndDeletedFalse("Existing Name")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category already exists with name: Existing Name");
    }

    @Test
    void deleteCategory_ShouldDeleteSuccessfully() {
        // Given
        when(categoryRepository.existsById(1L)).thenReturn(true);

        // When
        categoryService.deleteCategory(1L);

        // Then
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_ShouldThrowException_WhenNotFound() {
        // Given
        when(categoryRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");
    }

    @Test
    void getCategoryWithSubCategories_ShouldReturnCategory() {
        // Given
        when(categoryRepository.findByIdWithSubCategories(1L)).thenReturn(Optional.of(category));

        // When
        ResponseErrorTemplate response = categoryService.getCategoryWithSubCategories(1L);

        // Then
        assertThat(response).isNotNull();
        verify(categoryRepository).findByIdWithSubCategories(1L);
    }

    @Test
    void getAllCategoriesWithSubCategories_ShouldReturnList() {
        // Given
        when(categoryRepository.findAllWithSubCategories()).thenReturn(List.of(category));

        // When
        List<ResponseErrorTemplate> response = categoryService.getAllCategoriesWithSubCategories();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        verify(categoryRepository).findAllWithSubCategories();
    }
}
