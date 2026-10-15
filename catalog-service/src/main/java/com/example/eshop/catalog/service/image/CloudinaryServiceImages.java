package com.example.eshop.catalog.service.image;

import com.cloudinary.Cloudinary;
import com.example.eshop.catalog.service.CloudinaryService;
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
            String importKey=null;
            if(file instanceof com.example.eshop.common.imports.ImportImageFile && file.getOriginalFilename()!=null && file.getOriginalFilename().matches("import-[a-f0-9-]{36}-[a-f0-9]{64}\\.png")){
                importKey=file.getOriginalFilename().substring(0,file.getOriginalFilename().length()-4);
                options.put("public_id","imports/"+importKey);options.put("overwrite",false);options.put("unique_filename",false);
            }
            Map uploadResult = cloudinary.uploader()
                    .upload(file.getBytes(), options);
            String publicId = (String) uploadResult.get("public_id");
            if(publicId==null||publicId.isBlank()||(importKey!=null&&!publicId.endsWith("imports/"+importKey)))throw new IllegalStateException("Image provider did not confirm the requested asset");
            // Return the secure URL of the uploaded image without any transformation
            return cloudinary.url()
                    .secure(true)
                    .generate(publicId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}