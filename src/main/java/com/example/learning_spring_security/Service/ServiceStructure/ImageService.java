package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.Image;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ImageService {

   Image uploadImage(MultipartFile imageModel);

}
