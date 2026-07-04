package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.UserPermission;
import com.example.learning_spring_security.Repository.UserPermissionRepository;
import com.example.learning_spring_security.Service.ServiceStructure.UserPermissionService;
import com.example.learning_spring_security.ServiceMapper.UserPermissionMapper;
import com.example.learning_spring_security.dto.Request.GetUserPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.UserPermissionPageResponse;
import com.example.learning_spring_security.dto.Response.UserPermissionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserPermissionServiceImpl implements UserPermissionService {

    private final UserPermissionRepository userPermissionRepository;  // ← fixed repository

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getUserPermissions(GetUserPermissionRequest request) {
        log.info("getUserPermissions called: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(), request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                Sort.by("userPermissionId").descending()
        );

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        Page<UserPermission> page;
        String successMsg;

        if (type == null || type == 0 || value == null || value.isBlank()) {
            page = userPermissionRepository.findAll(pageable);
            successMsg = "Retrieved all permissions";

        } else if (type == 1) {
            page = userPermissionRepository.findByUserId(Long.parseLong(value), pageable);
            successMsg = "Retrieved permissions by user";

        } else if (type == 2) {
            page = userPermissionRepository.findByFuncId(Long.parseLong(value), pageable);
            successMsg = "Retrieved permissions by function";

        } else if (type == 3) {
            String[] parts = value.split(":");
            if (parts.length != 2) {
                throw new BadRequestException("criteriaValue for type 3 must be 'userId:funcId'");
            }
            page = userPermissionRepository.findByUserIdAndFuncId(
                    Long.parseLong(parts[0].trim()),
                    Long.parseLong(parts[1].trim()),
                    pageable
            );
            successMsg = "Retrieved permissions by user and function";

        } else if (type == 4) {
            Boolean isActive = Boolean.parseBoolean(value);
            page = userPermissionRepository.findByIsActive(isActive, pageable);
            successMsg = "Retrieved permissions by active status";

        } else if (type == 5) {
            String[] parts = value.split(":");
            if (parts.length != 2) {
                throw new BadRequestException("criteriaValue for type 5 must be 'userId:true/false'");
            }
            page = userPermissionRepository.findByUserIdAndIsActive(
                    Long.parseLong(parts[0].trim()),
                    Boolean.parseBoolean(parts[1].trim()),
                    pageable
            );
            successMsg = "Retrieved permissions by user and active status";

        } else {
            page = userPermissionRepository.findAll(pageable);
            successMsg = "Retrieved all permissions";
        }

        List<UserPermissionResponse> payload = page.getContent()
                .stream()
                .map(UserPermissionMapper::toResponse)
                .toList();

        UserPermissionPageResponse pageResponse = UserPermissionPageResponse.builder()
                .payload(payload)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .build();

        log.info("getUserPermissions completed: totalItems={}, totalPages={}",
                page.getTotalElements(), page.getTotalPages());

        String message = page.isEmpty() ? "No permissions found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getUserPermissionById(Long userPermissionId) {
        UserPermission permission = userPermissionRepository.findById(userPermissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + userPermissionId));
        return ResponseErrorTemplate.success("Permission retrieved successfully", UserPermissionMapper.toResponse(permission));
    }

    @Override
    public ResponseErrorTemplate createUserPermission(Long userId, Long funcId) {
        if (userPermissionRepository.existsByUserIdAndFuncId(userId, funcId)) {
            throw new BadRequestException("Permission already exists for user " + userId + " and function " + funcId);
        }
        UserPermission saved = userPermissionRepository.save(UserPermissionMapper.toEntity(userId, funcId));
        return ResponseErrorTemplate.success("Permission created successfully", UserPermissionMapper.toResponse(saved));
    }

    @Override
    public ResponseErrorTemplate updateUserPermission(Long userPermissionId, Boolean isActive) {
        UserPermission permission = userPermissionRepository.findById(userPermissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + userPermissionId));
        permission.setIsActive(isActive);
        UserPermission updated = userPermissionRepository.save(permission);
        return ResponseErrorTemplate.success("Permission updated successfully", UserPermissionMapper.toResponse(updated));
    }

    @Override
    public ResponseErrorTemplate deleteUserPermission(Long userPermissionId) {
        UserPermission permission = userPermissionRepository.findById(userPermissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + userPermissionId));
        userPermissionRepository.delete(permission);
        return ResponseErrorTemplate.success("Permission deleted successfully", null);
    }
}