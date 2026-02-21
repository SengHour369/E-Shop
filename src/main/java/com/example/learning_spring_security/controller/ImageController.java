//package com.example.learning_spring_security.controller;
//
//import com.example.learning_spring_security.Repository.ImageRepository;
//import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
//import com.example.learning_spring_security.dto.ImageModel;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//
//@RestController
//public class ImageController {
//
//    @Autowired
//    private ImageRepository imageRepository;
//
//    @Autowired
//    private ImageService imageService;
//
//    @PostMapping("/upload")
//    public ResponseEntity<Map> upload(ImageModel imageModel) {
//        try {
//            return imageService.uploadImage(imageModel);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//}