package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.Image;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ImageService {

   Image uploadImage(MultipartFile imageModel);

}
