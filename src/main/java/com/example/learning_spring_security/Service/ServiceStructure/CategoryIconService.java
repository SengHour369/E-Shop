package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryIconService {
    ResponseErrorTemplate uploadIcon(String name, MultipartFile file);
    ResponseErrorTemplate getIconById(Long id);
    List<ResponseErrorTemplate> getAllIcons();
    void deleteIcon(Long id);
}