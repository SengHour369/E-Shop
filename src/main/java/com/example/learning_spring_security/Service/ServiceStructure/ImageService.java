package com.example.learning_spring_security.Service.ServiceStructure;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ImageService {

   String uploadImage(MultipartFile imageModel);

}
