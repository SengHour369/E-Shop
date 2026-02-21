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
        try{
            HashMap<Object, Object> map = new HashMap<>();
            map.put("folderName", folderName);
            map.put("folder", folderName);
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),map);
            String publicId = (String) uploadResult.get("public_id");
            return cloudinary.url().secure(true).generate(publicId);
        } catch (IOException e) {
            throw new RuntimeException(e);

        }
    }
}
