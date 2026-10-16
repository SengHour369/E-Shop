package com.example.eshop.catalog.service;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.eshop.catalog.service.image.CloudinaryServiceImages;
import com.example.eshop.catalog.service.image.ImageServiceImpl;
import com.example.eshop.common.exception.CustomMessageException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CloudinaryUploadFailureTest {
    private final MockMultipartFile image = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});
    @Test void missingCredentialsReturnAnActionableError() {
        var service = new CloudinaryServiceImages();
        ReflectionTestUtils.setField(service,"cloudinary",new Cloudinary(Map.of("cloud_name","")));
        assertThatThrownBy(()->service.uploadFile(image,"products")).isInstanceOf(CustomMessageException.class).hasMessageContaining("not configured");
    }
    @Test void providerSecretsAreNeverReturnedToTheBrowser() throws Exception {
        var cloud = spy(new Cloudinary(Map.of("cloud_name","test-cloud","api_key","key","api_secret","secret")));
        var uploader=mock(Uploader.class);doReturn(uploader).when(cloud).uploader();
        when(uploader.upload(any(byte[].class),anyMap())).thenThrow(new RuntimeException("private-provider-secret"));
        var service=new CloudinaryServiceImages();ReflectionTestUtils.setField(service,"cloudinary",cloud);
        assertThatThrownBy(()->service.uploadFile(image,"products")).hasMessageContaining("Cloudinary upload failed").hasMessageNotContaining("private-provider-secret");
    }
    @Test void imageServicePropagatesFailureInsteadOfReturningNull() {
        var provider=mock(CloudinaryService.class);when(provider.uploadFile(any(),anyString())).thenThrow(new CustomMessageException("Image storage unavailable","502"));
        var service=new ImageServiceImpl();ReflectionTestUtils.setField(service,"cloudinaryService",provider);
        assertThatThrownBy(()->service.uploadImage(image)).hasMessage("Image storage unavailable");
    }
}
