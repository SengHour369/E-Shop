package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.AdminCreateUserRequest;
import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface UserService {

    @CacheEvict(value = "users", allEntries = true)
    ResponseErrorTemplate createUser(AdminCreateUserRequest request);

    @Cacheable(value = "users", key = "#id")
    ResponseErrorTemplate getUserById(Long id);

    @CacheEvict(value = "users", key = "#id")
    ResponseErrorTemplate updateUser(Long id, UserRequest request);

    @CacheEvict(value = "users", key = "#id")
    void deleteUser(Long id);

    @Cacheable(value = "users", key = "'all'")
    List<ResponseErrorTemplate> getAllUsers();

    @CacheEvict(value = "users", key = "#id")
    ResponseErrorTemplate changeUserStatus(Long id, String status);

    @CacheEvict(value = "users", key = "#userId")
    ResponseErrorTemplate updateProfilePicture(Long userId, MultipartFile profilePictureUrl);

    @Cacheable(value = "users", key = "'count'")
    Long countUsers();

    @Cacheable(value = "users", key = "#keyword + ':search'")
    List<ResponseErrorTemplate> searchUsers(String keyword);

    @CacheEvict(value = "users", key = "#userId")
    ResponseErrorTemplate changeUserPassword(Long userId, String oldPassword, String newPassword);
}