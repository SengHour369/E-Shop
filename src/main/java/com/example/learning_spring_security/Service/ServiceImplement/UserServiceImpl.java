package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.FunctionPermission;
import com.example.learning_spring_security.Model.Group;
import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Model.UserGroup;
import com.example.learning_spring_security.Model.UserPermission;
import com.example.learning_spring_security.Repository.FunctionPermissionRepository;
import com.example.learning_spring_security.Repository.GroupRepository;
import com.example.learning_spring_security.Repository.RoleRepository;
import com.example.learning_spring_security.Repository.UserGroupRepository;
import com.example.learning_spring_security.Repository.UserPermissionRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.ServiceImages.ImageServiceImpl;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.Service.ServiceStructure.UserService;
import com.example.learning_spring_security.ServiceMapper.UserMapper;
import com.example.learning_spring_security.dto.Request.AdminCreateUserRequest;
import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.AdminCreateUserResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class  UserServiceImpl implements UserService {
    final UserRepository userRepository;
    final RoleRepository roleRepository;
    final GroupRepository groupRepository;
    final UserGroupRepository userGroupRepository;
    final FunctionPermissionRepository functionPermissionRepository;
    final UserPermissionRepository userPermissionRepository;
    final ImageServiceImpl imageService;
    final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "users", allEntries = true)
    public ResponseErrorTemplate createUser(AdminCreateUserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password can't be blank or null");
        }
        if (userRepository.findFirstByUsernameOrEmail(request.username(), request.email()).isPresent()) {
            throw new BadRequestException("Username or Email already exists.");
        }
        if (request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new BadRequestException("At least one role must be assigned");
        }

        List<Role> roles = roleRepository.findAllById(request.roleIds());
        if (roles.size() != request.roleIds().size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }

        List<Long> groupIds = request.groupIds() == null ? Collections.emptyList() : request.groupIds();
        if (!groupIds.isEmpty()) {
            List<Group> groups = groupRepository.findAllById(groupIds);
            if (groups.size() != groupIds.size()) {
                throw new ResourceNotFoundException("One or more permission groups not found");
            }
            if (groups.stream().anyMatch(group -> Boolean.TRUE.equals(group.getIsDelete()))) {
                throw new BadRequestException("One or more permission groups are deleted and cannot be assigned");
            }
        }

        List<Long> permissionIds = request.permissionIds() == null ? Collections.emptyList() : request.permissionIds();
        if (!permissionIds.isEmpty()) {
            List<FunctionPermission> functions = functionPermissionRepository.findAllById(permissionIds);
            if (functions.size() != permissionIds.size()) {
                throw new ResourceNotFoundException("One or more permissions not found");
            }
            if (functions.stream().anyMatch(function -> Boolean.TRUE.equals(function.getIsDelete()))) {
                throw new BadRequestException("One or more permissions are deleted and cannot be assigned");
            }
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .roles(roles)
                .attempt(0)
                .status(Constant.ACT)
                .deleted(false)
                .enabled(true)
                .build();
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        if (!groupIds.isEmpty()) {
            List<UserGroup> userGroups = groupIds.stream()
                    .map(groupId -> UserGroup.builder()
                            .userId(savedUser.getId())
                            .groupId(groupId)
                            .isActive(true)
                            .isDelete(false)
                            .build())
                    .toList();
            userGroupRepository.saveAll(userGroups);
        }

        if (!permissionIds.isEmpty()) {
            List<UserPermission> userPermissions = permissionIds.stream()
                    .map(funcId -> UserPermission.builder()
                            .userId(savedUser.getId())
                            .funcId(funcId)
                            .isActive(true)
                            .isDelete(false)
                            .build())
                    .toList();
            userPermissionRepository.saveAll(userPermissions);
        }

        log.info("Admin created user {} with roles {}, groups {} and permissions {}", savedUser.getUsername(),
                roles.stream().map(Role::getName).toList(), groupIds, permissionIds);

        AdminCreateUserResponse response = AdminCreateUserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .fullName(savedUser.getFullName())
                .roles(roles.stream().map(Role::getName).toList())
                .groupIds(groupIds)
                .permissionIds(permissionIds)
                .created(savedUser.getCreatedAt())
                .build();

        return ResponseErrorTemplate.success("User created successfully", response);
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public ResponseErrorTemplate getUserById(Long id) {
        Optional<User> user = this.userRepository.findUserById(id);
        if(user.isEmpty()){
            log.error("User is not found by id :{}",id);
            throw new ResourceNotFoundException("User is not found by id");
        }
        log.info("User is found by id :{}",id);
        return UserMapper.toResponse(user.get());
    }


    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "users", allEntries = true)
    public ResponseErrorTemplate updateUser(Long id, UserRequest request) {
        Optional<User>  user = this.userRepository.findUserById(id);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User is not found", "id", id);
        }
        log.info("update user successfully by id is {}", id);
        user.get().setPassword(passwordEncoder.encode(request.getPassword()));
        user.get().setFullName(request.getFullName());
        user.get().setEmail(request.getEmail());
        user.get().setBirthdate(request.getBirthdate());

        return UserMapper.toResponse(this.userRepository.save(user.get()));
    }


    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id){
        Optional<User> user = this.userRepository.findUserById(id);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User is not found deleteUser ", "id", id);
        }
        log.info("Delete user successfully with id {}",id);
        user.get().setDeleted(true);
        this.userRepository.save(user.get());
    }

    @Override
    @Cacheable(value = "users", key = "'allUsers'")
    public List<ResponseErrorTemplate> getAllUsers() {
        log.info("get All Users");
        return  this.userRepository.findAllUser()
                .stream().map(UserMapper::toResponse)
                .toList();
    }


    @Override
    public ResponseErrorTemplate changeUserStatus(Long id, String status) {
        Optional<User> user = userRepository.findUserById(id);
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
        Optional<User> user = this.userRepository.findUserById(userId);
        if (user.isEmpty()) {
            log.info("User is not found updateProfilePicture by ID {}", userId);
            throw new ResourceNotFoundException("User is not found updateProfilePicture ", "id", userId);
        }
        log.info("Update profile picture successfully with id {}",userId);

        Image url = this.imageService.uploadImage(profilePictureUrl);
        user.get().setImage(url);
        return UserMapper.toResponse(userRepository.save(user.get()));
    }


    @Override
    public Long countUsers() {
        log.info("count users : {}", this.userRepository.findAllUser().size());
        return this.userRepository.count();
    }


    @Override
    @Cacheable(value = "users", key = "'search:' + #keyword")
    public List<ResponseErrorTemplate> searchUsers(String keyword) {
        log.info("search users by name :{}",keyword);
        return  this.userRepository.searchUsers(keyword)
                .stream().map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public ResponseErrorTemplate changeUserPassword(Long userId, String oldPassword, String newPassword) {
        Optional<User>  user = this.userRepository.findUserById(userId);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User is not found", "id", userId);
        }
        User user1 = user.get();
        if(!passwordEncoder.matches(oldPassword,user1.getPassword())){
            throw  new ResourceNotFoundException("Old Password Doesn't Match");
        }
        user1.setPassword(passwordEncoder.encode(newPassword));
        this.userRepository.save(user1);
        log.info("Password Changed Successfully for user ID :{}",user1.getId());
        return UserMapper.toResponse(user1);
    }
}