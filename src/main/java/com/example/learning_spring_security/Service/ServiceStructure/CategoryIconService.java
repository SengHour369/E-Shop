package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CategoryIconService {

    @CacheEvict(value = "categoryIcons", allEntries = true)
    ResponseErrorTemplate uploadIcon(String name, MultipartFile file);

    @Cacheable(value = "categoryIcons", key = "#id")
    ResponseErrorTemplate getIconById(Long id);

    @Cacheable(value = "categoryIcons", key = "'all'")
    List<ResponseErrorTemplate> getAllIcons();

    @CacheEvict(value = "categoryIcons", allEntries = true)
    void deleteIcon(Long id);
}