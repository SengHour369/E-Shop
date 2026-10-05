package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.constant.Constant;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.catalog.model.BrandLogo;
import com.example.eshop.catalog.model.Image;
import com.example.eshop.catalog.repository.BrandLogoRepository;
import com.example.eshop.catalog.service.image.ImageServiceImpl;
import com.example.eshop.catalog.service.BrandLogoService;
import com.example.eshop.catalog.dto.response.BrandLogoResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandLogoServiceImpl implements BrandLogoService {

    private final BrandLogoRepository brandLogoRepository;
    private final ImageServiceImpl imageService;

    @Override
    public ResponseErrorTemplate uploadLogo(String name, MultipartFile file) {
        Image uploaded = imageService.uploadImage(file);
        if (uploaded == null || uploaded.getUrl() == null) {
            throw new RuntimeException("Failed to upload brand logo");
        }

        BrandLogo logo = BrandLogo.builder()
                .name(name)
                .url(uploaded.getUrl())
                .build();

        BrandLogo saved = brandLogoRepository.save(logo);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getLogoById(Long id) {
        BrandLogo logo = brandLogoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand logo not found with id: " + id));
        return toResponse(logo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getAllLogos() {
        return brandLogoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteLogo(Long id) {
        if (!brandLogoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand logo not found with id: " + id);
        }
        brandLogoRepository.deleteById(id);
    }

    private ResponseErrorTemplate toResponse(BrandLogo logo) {
        BrandLogoResponse response = BrandLogoResponse.builder()
                .id(logo.getId())
                .name(logo.getName())
                .url(logo.getUrl())
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, response);
    }

}
