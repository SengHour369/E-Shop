package com.example.learning_spring_security.Service.ServiceImages;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImagesTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private com.cloudinary.Uploader uploader;

    @Mock
    private com.cloudinary.Url url;

    @InjectMocks
    private CloudinaryServiceImages cloudinaryService;

    private MultipartFile file;

    @BeforeEach
    void setUp() {
        file = mock(MultipartFile.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(cloudinary.url()).thenReturn(url);
    }

    @Test
    void uploadFile_ShouldUploadSuccessfully() throws IOException {
        // Given
        byte[] fileBytes = "test image content".getBytes();
        when(file.getBytes()).thenReturn(fileBytes);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("public_id", "test-public-id");
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        com.cloudinary.Transformation transformation = mock(com.cloudinary.Transformation.class);
        when(url.transformation(any(com.cloudinary.Transformation.class))).thenReturn(url);
        when(url.format(anyString())).thenReturn(url);
        when(url.secure(true)).thenReturn(url);
        when(url.generate("test-public-id")).thenReturn("https://cloudinary.com/test-image.png");

        // When
        String result = cloudinaryService.uploadFile(file, "test-folder");

        // Then
        assertThat(result).isEqualTo("https://cloudinary.com/test-image.png");
        verify(uploader).upload(fileBytes, Map.of("folder", "test-folder"));
        verify(url).generate("test-public-id");
    }

    @Test
    void uploadFile_ShouldThrowRuntimeException_WhenIOExceptionOccurs() throws IOException {
        // Given
        when(file.getBytes()).thenThrow(new IOException("File read error"));

        // When & Then
        assertThatThrownBy(() -> cloudinaryService.uploadFile(file, "test-folder"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }
}
