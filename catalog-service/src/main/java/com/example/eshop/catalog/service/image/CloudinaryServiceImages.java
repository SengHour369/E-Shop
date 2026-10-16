package com.example.eshop.catalog.service.image;

import com.cloudinary.Cloudinary;
import com.example.eshop.catalog.service.CloudinaryService;
import com.example.eshop.common.exception.CustomMessageException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryServiceImages implements CloudinaryService {
    @Resource private Cloudinary cloudinary;
    @Override
    public String uploadFile(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) throw new CustomMessageException("Choose a non-empty image file.", "400");
        if (!StringUtils.hasText(cloudinary.config.cloudName) || !StringUtils.hasText(cloudinary.config.apiKey) || !StringUtils.hasText(cloudinary.config.apiSecret)) {
            throw new CustomMessageException("Image uploads are not configured. Configure the Cloudinary cloud name, API key and API secret.", "503");
        }
        try {
            HashMap<String, Object> options = new HashMap<>();
            options.put("folder", folderName);
            options.put("connect_timeout", 10000);
            options.put("timeout", 30000);
            String importKey = null;
            if (file instanceof com.example.eshop.common.imports.ImportImageFile && file.getOriginalFilename() != null && file.getOriginalFilename().matches("import-[a-f0-9-]{36}-[a-f0-9]{64}\\.png")) {
                importKey = file.getOriginalFilename().substring(0, file.getOriginalFilename().length() - 4);
                options.put("public_id", "imports/" + importKey);
                options.put("overwrite", false);
                options.put("unique_filename", false);
            }
            Map result = cloudinary.uploader().upload(file.getBytes(), options);
            String publicId = (String) result.get("public_id");
            String secureUrl = (String) result.get("secure_url");
            if (!StringUtils.hasText(publicId) || (importKey != null && !publicId.endsWith("imports/" + importKey)) || !StringUtils.hasText(secureUrl) || !secureUrl.startsWith("https://")) {
                throw new CustomMessageException("Image provider did not confirm the uploaded image. Please retry.", "502");
            }
            return secureUrl;
        } catch (CustomMessageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            // Provider errors can contain authentication details; return a safe message.
            throw new CustomMessageException("Cloudinary upload failed. Check the configured cloud name and credentials, or try again shortly.", "502");
        }
    }
}
