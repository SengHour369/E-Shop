package com.example.eshop.catalog.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.eshop.catalog.service.image.CloudinaryServiceImages;
import com.example.eshop.common.imports.ImportImageFile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudinaryImportImageTest {
    @Test void retriesUseTheSameAssetWithoutOverwritingIt() throws Exception {
        Cloudinary cloud = spy(new Cloudinary(Map.of("cloud_name", "test-cloud", "api_key", "key", "api_secret", "secret")));
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);


        String key = "import-11111111-1111-4111-8111-111111111111-" + "a".repeat(64);
        String publicId = "products/imports/" + key;
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("public_id", publicId, "secure_url", "https://images.invalid/confirmed.png"));

        CloudinaryServiceImages service = new CloudinaryServiceImages();
        ReflectionTestUtils.setField(service, "cloudinary", cloud);
        ImportImageFile file = new ImportImageFile(key + ".png", new byte[]{1, 2});
        assertThat(service.uploadFile(file, "products")).endsWith("confirmed.png");
        assertThat(service.uploadFile(file, "products")).endsWith("confirmed.png");
        verify(uploader, times(2)).upload(any(byte[].class), eq(Map.of("folder", "products", "connect_timeout", 10000, "timeout", 30000, "public_id", "imports/" + key, "overwrite", false, "unique_filename", false)));
    }

    @Test void missingProviderAcknowledgementFailsClosed() throws Exception {
        Cloudinary cloud = spy(new Cloudinary(Map.of("cloud_name", "test-cloud", "api_key", "key", "api_secret", "secret")));
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of());
        CloudinaryServiceImages service = new CloudinaryServiceImages();
        ReflectionTestUtils.setField(service, "cloudinary", cloud);
        assertThatThrownBy(() -> service.uploadFile(new ImportImageFile("photo.png", new byte[]{1}), "products"))
                .isInstanceOf(com.example.eshop.common.exception.CustomMessageException.class).hasMessageContaining("did not confirm");
        verify(cloud, never()).url();
    }
}
