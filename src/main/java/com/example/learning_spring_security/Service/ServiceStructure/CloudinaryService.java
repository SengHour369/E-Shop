package com.example.learning_spring_security.Service.ServiceStructure;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadFile(MultipartFile file, String folderName);
}
