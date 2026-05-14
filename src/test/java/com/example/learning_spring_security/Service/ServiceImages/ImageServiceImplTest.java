package com.example.learning_spring_security.Service.ServiceImages;

import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Repository.ImageRepository;
import com.example.learning_spring_security.Service.ServiceStructure.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private ImageServiceImpl imageService;

    private MultipartFile file;

    @BeforeEach
    void setUp() {
        file = mock(MultipartFile.class);
    }

    @Test
    void uploadImage_ShouldUploadSuccessfully() {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadFile(file, "folder_1")).thenReturn("https://cloudinary.com/test-image.png");
        when(imageRepository.save(any(Image.class))).thenReturn(new Image());

        // When
        String result = imageService.uploadImage(file);

        // Then
        assertThat(result).isEqualTo("https://cloudinary.com/test-image.png");
        verify(cloudinaryService).uploadFile(file, "folder_1");
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    void uploadImage_ShouldReturnNull_WhenFileIsEmpty() {
        // Given
        when(file.isEmpty()).thenReturn(true);

        // When
        String result = imageService.uploadImage(file);

        // Then
        assertThat(result).isNull();
        verifyNoInteractions(cloudinaryService, imageRepository);
    }

    @Test
    void uploadImage_ShouldReturnNull_WhenCloudinaryReturnsNull() {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadFile(file, "folder_1")).thenReturn(null);

        // When
        String result = imageService.uploadImage(file);

        // Then
        assertThat(result).isNull();
        verify(cloudinaryService).uploadFile(file, "folder_1");
        verifyNoInteractions(imageRepository);
    }

    @Test
    void uploadImage_ShouldReturnNull_WhenExceptionOccurs() {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadFile(file, "folder_1")).thenThrow(new RuntimeException("Upload failed"));

        // When
        String result = imageService.uploadImage(file);

        // Then
        assertThat(result).isNull();
        verify(cloudinaryService).uploadFile(file, "folder_1");
        verifyNoInteractions(imageRepository);
    }
}
