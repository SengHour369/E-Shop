package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BrandLogoService {
    ResponseErrorTemplate uploadLogo(String name, MultipartFile file);
    ResponseErrorTemplate getLogoById(Long id);
    List<ResponseErrorTemplate> getAllLogos();
    void deleteLogo(Long id);
}
