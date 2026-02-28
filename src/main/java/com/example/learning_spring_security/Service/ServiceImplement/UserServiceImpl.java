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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
   final UserRepository userRepository;
   final ImageService imageService;
   final PasswordEncoder passwordEncoder;

    @Override
    public ResponseErrorTemplate getUserById(Long id) {
        Optional<User> user = this.userRepository.findById(id);
        if(user.isEmpty()){
            log.error("User is not found by id :{}",id);
            throw new ResourceNotFoundException("User is not found by id");
        }
        log.info("User is found by id :{}",id);
        return UserMapper.toResponse(user.get());
    }


    @Override
    public ResponseErrorTemplate updateUser(Long id, UserRequest request) {
        Optional<User>  user = this.userRepository.findById(id);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User is not found", "id", id);
        }
        log.info("update user successfully by id is {}", id);
            user.get().setPassword(request.getPassword());
            user.get().setFullName(request.getFullName());
            user.get().setEmail(request.getEmail());
            user.get().setBirthdate(request.getBirthdate());

        return UserMapper.toResponse(this.userRepository.save(user.get()));
    }


    @Override
    public void deleteUser(Long id){
        Optional<User> user = this.userRepository.findById(id);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User is not found deleteUser ", "id", id);
        }
        log.info("Delete user successfully with id {}",id);
        this.userRepository.deleteById(user.get().getId());
    }

    @Override
    public List<ResponseErrorTemplate> getAllUsers() {
        log.info("get All Users");
        return  this.userRepository.findAll()
                .stream().map(UserMapper::toResponse)
                .toList();
    }


    @Override
    public ResponseErrorTemplate changeUserStatus(Long id, String status) {
        Optional<User> user = userRepository.findById(id);
               if(user.isEmpty()) {
                   log.info("User is not found changeUserStatus by ID {}", id);
                   throw new ResourceNotFoundException("User is not found changeUserStatus ", "id", id);
               }
               log.info("Change user status successfully by ID {}", id);
        user.get().setStatus(status);
        userRepository.save(user.get());
        return UserMapper.toResponse(user.get());
    }


    @Override
    public ResponseErrorTemplate updateProfilePicture(Long userId, MultipartFile profilePictureUrl) {
        Optional<User> user = this.userRepository.findById(userId);
        if (user.isEmpty()) {
            log.info("User is not found updateProfilePicture by ID {}", userId);
            throw new ResourceNotFoundException("User is not found updateProfilePicture ", "id", userId);
        }
        log.info("Update profile picture successfully with id {}",userId);

        String url = this.imageService.uploadImage(profilePictureUrl);
        user.get().setImage(url);
        return UserMapper.toResponse(userRepository.save(user.get()));
    }


    @Override
    public Long countUsers() {
        log.info("count users : {}", this.userRepository.findAll().size());
        return this.userRepository.count();
    }


    @Override
    public List<ResponseErrorTemplate> searchUsers(String keyword) {
         log.info("search users by name :{}",keyword);
         return  this.userRepository.searchUsers(keyword)
                 .stream().map(UserMapper::toResponse)
                 .toList();
    }

    @Override
    public ResponseErrorTemplate changeUserPassword(Long userId, String oldPassword, String newPassword) {
           Optional<User>  user = this.userRepository.findById(userId);
           if (user.isEmpty()) {
               throw new ResourceNotFoundException("User is not found", "id", userId);
           }
           User user1 = user.get();
           if(!passwordEncoder.matches(oldPassword,user1.getPassword())){
               user1.setPassword(newPassword);
              throw  new ResourceNotFoundException("Old Password Doesn't Match");
           }
           user1.setPassword(newPassword);
           this.userRepository.save(user1);
           log.info("Password Changed Successfully for user ID :{}",user1.getId());
           return UserMapper.toResponse(user1);
    }
}
