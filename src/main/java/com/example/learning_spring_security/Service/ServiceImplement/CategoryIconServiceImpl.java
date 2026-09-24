package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.CategoryIcon;
import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Repository.CategoryIconRepository;
import com.example.learning_spring_security.Service.ServiceImages.ImageServiceImpl;
import com.example.learning_spring_security.Service.ServiceStructure.CategoryIconService;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.dto.Response.CategoryIconResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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