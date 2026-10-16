package com.example.eshop.catalog.service.image;


import com.example.eshop.catalog.model.Image;
import com.example.eshop.catalog.service.CloudinaryService;
import com.example.eshop.catalog.service.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;


@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    @Autowired
    private CloudinaryService cloudinaryService;



    @Override
    public Image uploadImage(MultipartFile imageModel) {
        if (imageModel == null || imageModel.isEmpty()) {
            throw new com.example.eshop.common.exception.CustomMessageException("Choose a non-empty image file.", "400");
        }
        Image image = new Image();
        image.setUrl(cloudinaryService.uploadFile(imageModel, "folder_1"));
        if (image.getUrl() == null || image.getUrl().isBlank()) {
            throw new com.example.eshop.common.exception.CustomMessageException("Image provider did not confirm the uploaded image.", "502");
        }
        return image;
    }

}