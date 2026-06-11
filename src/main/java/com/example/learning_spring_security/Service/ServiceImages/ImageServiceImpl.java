package com.example.learning_spring_security.Service.ServiceImages;


import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Service.ServiceStructure.CloudinaryService;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
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
        try {

            if (imageModel.isEmpty()) {
                return null;
            }

            Image image = new Image();

            image.setUrl(cloudinaryService.uploadFile(imageModel, "folder_1"));
            if(image.getUrl() == null) {
                return null;
            }
            // Do NOT persist the Image here. We only upload to Cloudinary and return an Image entity
            // that will be attached to a Product and persisted together with the Product (cascade = ALL).
            return image;
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary", e);
            return null;
        }


    }
}