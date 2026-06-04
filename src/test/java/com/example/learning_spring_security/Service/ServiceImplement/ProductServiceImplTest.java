//package com.example.learning_spring_security.Service.ServiceImplement;
//
//import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
//import com.example.learning_spring_security.Model.Product;
//import com.example.learning_spring_security.Model.ProductSku;
//import com.example.learning_spring_security.Model.SubCategory;
//import com.example.learning_spring_security.Repository.ProductRepository;
//import com.example.learning_spring_security.Repository.ProductSkuRepository;
//import com.example.learning_spring_security.Repository.SubCategoryRepository;
//import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
//import com.example.learning_spring_security.dto.Request.ProductRequest;
//import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ProductServiceImplTest {
//
//    @Mock
//    private ProductRepository productRepository;
//
//    @Mock
//    private ProductSkuRepository productSkuRepository;
//
//    @Mock
//    private SubCategoryRepository subCategoryRepository;
//
//    @Mock
//    private ImageService imageService;
//
//    @InjectMocks
//    private ProductServiceImpl productService;
//
//    private Product product;
//    private SubCategory subCategory;
//    private ProductRequest productRequest;
//    private Pageable pageable;
//
//    @BeforeEach
//    void setUp() {
//        subCategory = SubCategory.builder().id(1L).name("Electronics").build();
//        product = Product.builder()
//                .id(1L)
//                .name("Test Product")
//                .description("Test Description")
//                .subCategory(subCategory)
//                .isActive(true)
//                .build();
//        productRequest = ProductRequest.builder()
//                .name("Test Product")
//                .description("Test Description")
//                .subCategoryId(1L)
//                .build();
//        pageable = PageRequest.of(0, 10);
//    }
//
//    @Test
//    void createProduct_ShouldCreateProductSuccessfully() throws Exception {
//        // Given
//        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));
//        when(productRepository.save(any(Product.class))).thenReturn(product);
//
//        // When
//        ResponseErrorTemplate response = productService.createProduct(productRequest);
//
//        // Then
//        assertThat(response).isNotNull();
//        verify(productRepository).save(any(Product.class));
//        verify(subCategoryRepository).findById(1L);
//    }
//
//    @Test
//    void createProduct_ShouldThrowException_WhenSubCategoryNotFound() {
//        // Given
//        when(subCategoryRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() -> productService.createProduct(productRequest))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("SubCategory not found with id: 1");
//    }
//
//    @Test
//    void getProductById_ShouldReturnProduct_WhenExists() {
//        // Given
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//
//        // When
//        ResponseErrorTemplate response = productService.getProductById(1L);
//
//        // Then
//        assertThat(response).isNotNull();
//        verify(productRepository).findById(1L);
//    }
//
//    @Test
//    void getProductById_ShouldThrowException_WhenProductNotFound() {
//        // Given
//        when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() -> productService.getProductById(1L))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("Product not found with id: 1");
//    }
//
//    @Test
//    void getProductWithSkus_ShouldReturnProduct_WhenExists() {
//        // Given
//        when(productRepository.findByIdWithSkus(1L)).thenReturn(Optional.of(product));
//
//        // When
//        ResponseErrorTemplate response = productService.getProductWithSkus(1L);
//
//        // Then
//        assertThat(response).isNotNull();
//        verify(productRepository).findByIdWithSkus(1L);
//    }
//
//    @Test
//    void getAllProducts_ShouldReturnPagedProducts() {
//        // Given
//        Page<Product> productPage = new PageImpl<>(List.of(product));
//        when(productRepository.findAll(pageable)).thenReturn(productPage);
//
//        // When
//        Page<ResponseErrorTemplate> response = productService.getAllProducts(pageable);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//        verify(productRepository).findAll(pageable);
//    }
//
//    @Test
//    void getActiveProducts_ShouldReturnPagedActiveProducts() {
//        // Given
//        Page<Product> productPage = new PageImpl<>(List.of(product));
//        when(productRepository.findByIsActiveTrue(pageable)).thenReturn(productPage);
//
//        // When
//        Page<ResponseErrorTemplate> response = productService.getActiveProducts(pageable);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//        verify(productRepository).findByIsActiveTrue(pageable);
//    }
//
//    @Test
//    void addImageToProduct_ShouldAddImageSuccessfully() {
//        // Given
//        MultipartFile file = mock(MultipartFile.class);
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(imageService.uploadImage(file)).thenReturn("imageUrl");
//        when(productRepository.save(any(Product.class))).thenReturn(product);
//
//        // When
//        ResponseErrorTemplate response = productService.addImageToProduct(1L, file);
//
//        // Then
//        assertThat(response).isNotNull();
//        verify(imageService).uploadImage(file);
//        verify(productRepository).save(product);
//    }
//
//    @Test
//    void addImageToProduct_ShouldThrowException_WhenProductNotFound() {
//        // Given
//        MultipartFile file = mock(MultipartFile.class);
//        when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() -> productService.addImageToProduct(1L, file))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("Product not found with id: 1");
//    }
//
//    @Test
//    void getProductsBySubCategory_ShouldReturnProducts_WhenSubCategoryExists() {
//        // Given
//        Page<Product> productPage = new PageImpl<>(List.of(product));
//        when(subCategoryRepository.existsById(1L)).thenReturn(true);
//        when(productRepository.findBySubCategoryId(1L, pageable)).thenReturn(productPage);
//
//        // When
//        Page<ResponseErrorTemplate> response = productService.getProductsBySubCategory(1L, pageable);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//    }
//
//    @Test
//    void getProductsBySubCategory_ShouldThrowException_WhenSubCategoryNotFound() {
//        // Given
//        when(subCategoryRepository.existsById(1L)).thenReturn(false);
//
//        // When & Then
//        assertThatThrownBy(() -> productService.getProductsBySubCategory(1L, pageable))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("SubCategory not found with id: 1");
//    }
//
//    @Test
//    void getProductsByCategory_ShouldReturnProducts() {
//        // Given
//        Page<Product> productPage = new PageImpl<>(List.of(product));
//        when(productRepository.findByCategoryId(1L, pageable)).thenReturn(productPage);
//
//        // When
//        Page<ResponseErrorTemplate> response = productService.getProductsByCategory(1L, pageable);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//    }
//
//    @Test
//    void searchProducts_ShouldReturnProducts() {
//        // Given
//        Page<Product> productPage = new PageImpl<>(List.of(product));
//        when(productRepository.searchProducts("test", pageable)).thenReturn(productPage);
//
//        // When
//        Page<ResponseErrorTemplate> response = productService.searchProducts("test", pageable);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//    }
//
//    @Test
//    void updateProduct_ShouldUpdateSuccessfully() {
//        // Given
//        ProductRequest updateRequest = ProductRequest.builder()
//                .name("Updated Product")
//                .description("Updated Description")
//                .subCategoryId(1L)
//                .build();
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));
//        when(productRepository.save(any(Product.class))).thenReturn(product);
//
//        // When
//        ResponseErrorTemplate response = productService.updateProduct(1L, updateRequest);
//
//        // Then
//        assertThat(response).isNotNull();
//        verify(productRepository).save(any(Product.class));
//    }
//
//    @Test
//    void updateProduct_ShouldThrowException_WhenProductNotFound() {
//        // Given
//        ProductRequest updateRequest = ProductRequest.builder().build();
//        when(productRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("Product not found with id: 1");
//    }
//
//    @Test
//    void deleteProduct_ShouldDeleteSuccessfully() {
//        // Given
//        when(productRepository.existsById(1L)).thenReturn(true);
//
//        // When
//        productService.deleteProduct(1L);
//
//        // Then
//        verify(productRepository).deleteById(1L);
//    }
//
//    @Test
//    void deleteProduct_ShouldThrowException_WhenProductNotFound() {
//        // Given
//        when(productRepository.existsById(1L)).thenReturn(false);
//
//        // When & Then
//        assertThatThrownBy(() -> productService.deleteProduct(1L))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("Product not found with id: 1");
//    }
//
//    @Test
//    void updateProductStatus_ShouldUpdateStatusSuccessfully() {
//        // Given
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(productRepository.save(any(Product.class))).thenReturn(product);
//
//        // When
//        ResponseErrorTemplate response = productService.updateProductStatus(1L, false);
//
//        // Then
//        assertThat(response).isNotNull();
//        verify(productRepository).save(product);
//    }
//}
