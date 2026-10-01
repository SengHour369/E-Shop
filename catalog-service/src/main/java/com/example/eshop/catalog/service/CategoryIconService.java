package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryIconService {
    ResponseErrorTemplate uploadIcon(String name, MultipartFile file);
    ResponseErrorTemplate getIconById(Long id);
    List<ResponseErrorTemplate> getAllIcons();
    void deleteIcon(Long id);
}