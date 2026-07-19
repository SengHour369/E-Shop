package com.example.learning_spring_security.Service.ServiceImages;

import com.cloudinary.Cloudinary;
import com.example.learning_spring_security.Service.ServiceStructure.CloudinaryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryServiceImages implements CloudinaryService {
    @Resource
    private Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folderName) {
        try {
            HashMap<String, Object> options = new HashMap<>();
            options.put("folder", folderName);
            Map uploadResult = cloudinary.uploader()
                    .upload(file.getBytes(), options);
            String publicId = (String) uploadResult.get("public_id");
            // Return the secure URL of the uploaded image without any transformation
            return cloudinary.url()
                    .secure(true)
                    .generate(publicId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}