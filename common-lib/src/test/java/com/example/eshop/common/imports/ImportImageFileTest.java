package com.example.eshop.common.imports;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
class ImportImageFileTest {
 @Test void acceptsPixelsAndProducesImmutablePng()throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"jpeg",out);var clean=ImportImageFile.sanitize(new MockMultipartFile("file","../../image.jpg","image/jpeg",out.toByteArray()));assertThat(clean.getContentType()).isEqualTo("image/png");assertThat(clean.getOriginalFilename()).isEqualTo("product.png");assertThat(clean.checksum()).hasSize(64);byte[] edited=clean.getBytes();edited[0]=0;assertThat(clean.getBytes()[0]).isNotZero();}
 @Test void rejectsMimeSpoofingSvgAndOversizedContent(){assertThatThrownBy(()->ImportImageFile.sanitize(new MockMultipartFile("file","fake.png","image/png","<svg onload='alert(1)'/>".getBytes()))).hasMessageContaining("valid PNG/JPEG");assertThatThrownBy(()->ImportImageFile.sanitize(new MockMultipartFile("file","large.png","image/png",new byte[5_242_881]))).hasMessageContaining("valid PNG/JPEG");}
 @Test void rejectsPixelBombBeforeDecoding()throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(3200,3200,BufferedImage.TYPE_BYTE_GRAY),"png",out);assertThatThrownBy(()->ImportImageFile.sanitize(new MockMultipartFile("file","pixels.png","image/png",out.toByteArray()))).hasMessageContaining("10 million pixels");}
}
