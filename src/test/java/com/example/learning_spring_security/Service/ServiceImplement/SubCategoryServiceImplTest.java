package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Category;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.Repository.CategoryRepository;
import com.example.learning_spring_security.Repository.SubCategoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubCategoryServiceImplTest {

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private SubCategoryServiceImpl subCategoryService;

    private SubCategory subCategory;
    private Category category;
    private SubCategoryRequest subCategoryRequest;
    private Pageable pageable;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();
        subCategory = SubCategory.builder()
                .id(1L)
                .name("Smartphones")
                .description("Mobile phones")
                .category(category)
                .image("image.jpg")

                .build();
        subCategoryRequest = SubCategoryRequest.builder()
                .name("Smartphones")
                .description("Mobile phones")
                .categoryId(1L)
                .build();
        pageable = PageRequest.of(0, 10);
        file = mock(MultipartFile.class);
    }

    @Test
    void createSubCategory_ShouldCreateSuccessfully() throws Exception {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(subCategoryRepository.existsByNameAndCategoryId("Smartphones", 1L)).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(imageService.uploadImage(file)).thenReturn("uploaded-image.jpg");
        when(subCategoryRepository.save(any(SubCategory.class))).thenReturn(subCategory);

        // When
        ResponseErrorTemplate response = subCategoryService.createSubCategory(subCategoryRequest, file);

        // Then
        assertThat(response).isNotNull();
        verify(imageService).uploadImage(file);
        verify(subCategoryRepository).save(any(SubCategory.class));
    }

    @Test
    void createSubCategory_ShouldThrowException_WhenCategoryNotFound() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subCategoryService.createSubCategory(subCategoryRequest, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");
    }

    @Test
    void createSubCategory_ShouldThrowException_WhenSubCategoryExists() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(subCategoryRepository.existsByNameAndCategoryId("Smartphones", 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> subCategoryService.createSubCategory(subCategoryRequest, file))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("SubCategory already exists with name: Smartphones in this category");
    }

    @Test
    void createSubCategory_ShouldThrowException_WhenFileIsEmpty() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(subCategoryRepository.existsByNameAndCategoryId("Smartphones", 1L)).thenReturn(false);
        when(file.isEmpty()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> subCategoryService.createSubCategory(subCategoryRequest, file))
                .isInstanceOf(Exception.class)
                .hasMessage("file in image is empty");
    }

    @Test
    void getSubCategoryById_ShouldReturnSubCategory_WhenExists() {
        // Given
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));

        // When
        ResponseErrorTemplate response = subCategoryService.getSubCategoryById(1L);

        // Then
        assertThat(response).isNotNull();
        verify(subCategoryRepository).findById(1L);
    }

    @Test
    void getSubCategoryById_ShouldThrowException_WhenNotFound() {
        // Given
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subCategoryService.getSubCategoryById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("SubCategory not found with id: 1");
    }

    @Test
    void getSubCategoryAll_ShouldReturnPagedSubCategories() {
        // Given
        Page<SubCategory> subCategoryPage = new PageImpl<>(List.of(subCategory));
        when(subCategoryRepository.findAll(pageable)).thenReturn(subCategoryPage);

        // When
        Page<ResponseErrorTemplate> response = subCategoryService.getSubCategoryAll(pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(subCategoryRepository).findAll(pageable);
    }

    @Test
    void getSubCategoriesByCategoryAsList_ShouldReturnList() {
        // Given
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(subCategoryRepository.findByCategoryId(1L)).thenReturn(List.of(subCategory));

        // When
        List<ResponseErrorTemplate> response = subCategoryService.getSubCategoriesByCategoryAsList(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
    }

    @Test
    void getSubCategoriesByCategoryAsList_ShouldThrowException_WhenCategoryNotFound() {
        // Given
        when(categoryRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> subCategoryService.getSubCategoriesByCategoryAsList(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");
    }

    @Test
    void updateSubCategory_ShouldUpdateSuccessfully() {
        // Given
        SubCategoryRequest updateRequest = SubCategoryRequest.builder()
                .name("Updated Smartphones")
                .description("Updated description")
                .categoryId(1L)
                .build();
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));
        when(subCategoryRepository.existsByNameAndCategoryId("Updated Smartphones", 1L)).thenReturn(false);
        when(subCategoryRepository.save(any(SubCategory.class))).thenReturn(subCategory);

        // When
        ResponseErrorTemplate response = subCategoryService.updateSubCategory(1L, updateRequest, file);

        // Then
        assertThat(response).isNotNull();
        verify(subCategoryRepository).save(any(SubCategory.class));
    }

    @Test
    void updateSubCategory_ShouldThrowException_WhenSubCategoryNotFound() {
        // Given
        SubCategoryRequest updateRequest = SubCategoryRequest.builder().build();
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subCategoryService.updateSubCategory(1L, updateRequest, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("SubCategory not found with id: 1");
    }

    @Test
    void updateSubCategory_ShouldThrowException_WhenNewNameExists() {
        // Given
        SubCategoryRequest updateRequest = SubCategoryRequest.builder()
                .name("Existing Name")
                .categoryId(1L)
                .build();
        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));
        when(subCategoryRepository.existsByNameAndCategoryId("Existing Name", 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> subCategoryService.updateSubCategory(1L, updateRequest, file))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("SubCategory already exists with name: Existing Name in this category");
    }

    @Test
    void deleteSubCategory_ShouldDeleteSuccessfully() {
        // Given
        when(subCategoryRepository.existsById(1L)).thenReturn(true);

        // When
        subCategoryService.deleteSubCategory(1L);

        // Then
        verify(subCategoryRepository).deleteById(1L);
    }

    @Test
    void deleteSubCategory_ShouldThrowException_WhenNotFound() {
        // Given
        when(subCategoryRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> subCategoryService.deleteSubCategory(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("SubCategory not found with id: 1");
    }

    @Test
    void getSubCategoryWithProducts_ShouldReturnSubCategory() {
        // Given
        when(subCategoryRepository.findByIdWithProducts(1L)).thenReturn(Optional.of(subCategory));

        // When
        ResponseErrorTemplate response = subCategoryService.getSubCategoryWithProducts(1L);

        // Then
        assertThat(response).isNotNull();
        verify(subCategoryRepository).findByIdWithProducts(1L);
    }
}
