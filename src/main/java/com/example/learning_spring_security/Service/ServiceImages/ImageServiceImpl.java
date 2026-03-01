package com.example.learning_spring_security.Service.ServiceImages;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Repository.ImageRepository;
import com.example.learning_spring_security.Service.ServiceStructure.CloudinaryService;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
@Service
public class ImageServiceImpl implements ImageService {

    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private ImageRepository imageRepository;



    @Override
    public String uploadImage(MultipartFile imageModel) {
        try {

            if (imageModel.isEmpty()) {
                return null;
            }

            Image image = new Image();
            image.setUrl(cloudinaryService.uploadFile(imageModel, "folder_1"));
            if(image.getUrl() == null) {
                return null;
            }
            imageRepository.save(image);
            return image.getUrl();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }
}
