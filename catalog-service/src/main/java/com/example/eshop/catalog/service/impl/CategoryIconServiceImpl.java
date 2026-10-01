package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.constant.Constant;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.catalog.model.CategoryIcon;
import com.example.eshop.catalog.model.Image;
import com.example.eshop.catalog.repository.CategoryIconRepository;
import com.example.eshop.catalog.service.image.ImageServiceImpl;
import com.example.eshop.catalog.service.CategoryIconService;
import com.example.eshop.catalog.service.ImageService;
import com.example.eshop.catalog.dto.response.CategoryIconResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryIconServiceImpl implements CategoryIconService {

    private final CategoryIconRepository categoryIconRepository;
    private final ImageServiceImpl imageService;

    @Override
    public ResponseErrorTemplate uploadIcon(String name, MultipartFile file) {
        Image uploaded = imageService.uploadImage(file);
        if (uploaded == null || uploaded.getUrl() == null) {
            throw new RuntimeException("Failed to upload icon image");
        }

        CategoryIcon icon = CategoryIcon.builder()
                .name(name)
                .url(uploaded.getUrl())
                .build();

        CategoryIcon saved = categoryIconRepository.save(icon);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getIconById(Long id) {
        CategoryIcon icon = categoryIconRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category icon not found with id: " + id));
        return toResponse(icon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getAllIcons() {
        return categoryIconRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteIcon(Long id) {
        if (!categoryIconRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category icon not found with id: " + id);
        }
        categoryIconRepository.deleteById(id);
    }

    private ResponseErrorTemplate toResponse(CategoryIcon icon) {
        CategoryIconResponse response = CategoryIconResponse.builder()
                .id(icon.getId())
                .name(icon.getName())
                .url(icon.getUrl())
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, response);
    }



}