package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.Service.ServiceStructure.UserService;
import com.example.learning_spring_security.ServiceMapper.UserMapper;
import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
   final UserRepository userRepository;
   final ImageService imageService;

    @Override
    public ResponseErrorTemplate getUserById(Long id) {
        User user = this.userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User is not found", "id", id));
        return UserMapper.toResponse(user);
    }


    @Override
    public ResponseErrorTemplate updateUser(Long id, UserRequest request) {
        Optional<User>  user = this.userRepository.findById(id);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User is not found", "id", id);
        }
            user.get().setPassword(request.getPassword());
            user.get().setFullName(request.getFullName());
            user.get().setEmail(request.getEmail());
            user.get().setBirthdate(request.getBirthdate());

        return UserMapper.toResponse(this.userRepository.save(user.get()));
    }


    @Override
    public void deleteUser(Long id){
        User user = this.userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User is not found", "id", id));
        this.userRepository.deleteById(user.getId());
    }

    @Override
    public List<ResponseErrorTemplate> getAllUsers() {
        return  this.userRepository.findAll()
                .stream().map(UserMapper::toResponse)
                .toList();
    }


    @Override
    public ResponseErrorTemplate changeUserStatus(Long id, String status) {
        Optional<User> user = Optional.of(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User is found")));
        user.get().setStatus(status);
        userRepository.save(user.get());
        return UserMapper.toResponse(user.get());
    }


    @Override
    public ResponseErrorTemplate updateProfilePicture(Long userId, MultipartFile profilePictureUrl) {
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User is found"));
        String url = this.imageService.uploadImage(profilePictureUrl);
        user.setImage(url);
        return UserMapper.toResponse(userRepository.save(user));
    }


    @Override
    public Long countUsers() {
        return this.userRepository.count();
    }


    @Override
    public List<ResponseErrorTemplate> searchUsers(String keyword) {
         return  this.userRepository.searchUsers(keyword)
                 .stream().map(UserMapper::toResponse)
                 .toList();
    }
}
