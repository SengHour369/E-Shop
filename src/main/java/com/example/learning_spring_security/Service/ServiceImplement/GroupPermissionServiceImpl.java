package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.GroupPermission;
import com.example.learning_spring_security.Repository.GroupPermissionRepository;
import com.example.learning_spring_security.Service.ServiceStructure.GroupPermissionService;
import com.example.learning_spring_security.ServiceMapper.GroupPermissionMapper;
import com.example.learning_spring_security.dto.Request.GetGroupPermissionRequest;
import com.example.learning_spring_security.dto.Response.GroupPermissionPageResponse;
import com.example.learning_spring_security.dto.Response.GroupPermissionResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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
public class GroupPermissionServiceImpl implements GroupPermissionService {

    private final GroupPermissionRepository groupPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getGroupPermissions(GetGroupPermissionRequest request) {
        log.info("getGroupPermissions called: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(), request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                Sort.by("groupPermissionId").descending()
        );

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        Page<GroupPermission> page;
        String successMsg;

        if (type == null || type == 0 || value == null || value.isBlank()) {
            page = groupPermissionRepository.findAll(pageable);
            successMsg = "Retrieved all group permissions";

        } else if (type == 1) {
            page = groupPermissionRepository.findByGroupId(Long.parseLong(value), pageable);
            successMsg = "Retrieved permissions by group";

        } else if (type == 2) {
            page = groupPermissionRepository.findByFuncId(Long.parseLong(value), pageable);
            successMsg = "Retrieved permissions by function";

        } else if (type == 3) {
            // criteriaValue format: "groupId:funcId"
            String[] parts = value.split(":");
            if (parts.length != 2) {
                throw new BadRequestException("criteriaValue for type 3 must be 'groupId:funcId'");
            }
            page = groupPermissionRepository.findByGroupIdAndFuncId(
                    Long.parseLong(parts[0].trim()),
                    Long.parseLong(parts[1].trim()),
                    pageable
            );
            successMsg = "Retrieved permissions by group and function";

        } else if (type == 4) {
            Boolean isActive = Boolean.parseBoolean(value);
            page = groupPermissionRepository.findByIsActive(isActive, pageable);
            successMsg = "Retrieved permissions by active status";

        } else if (type == 5) {
            // criteriaValue format: "groupId:true" or "groupId:false"
            String[] parts = value.split(":");
            if (parts.length != 2) {
                throw new BadRequestException("criteriaValue for type 5 must be 'groupId:true/false'");
            }
            page = groupPermissionRepository.findByGroupIdAndIsActive(
                    Long.parseLong(parts[0].trim()),
                    Boolean.parseBoolean(parts[1].trim()),
                    pageable
            );
            successMsg = "Retrieved permissions by group and active status";

        } else {
            page = groupPermissionRepository.findAll(pageable);
            successMsg = "Retrieved all group permissions";
        }

        List<GroupPermissionResponse> payload = page.getContent()
                .stream()
                .map(GroupPermissionMapper::toResponse)
                .toList();

        GroupPermissionPageResponse pageResponse = GroupPermissionPageResponse.builder()
                .payload(payload)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .build();

        log.info("getGroupPermissions completed: totalItems={}, totalPages={}",
                page.getTotalElements(), page.getTotalPages());

        String message = page.isEmpty() ? "No group permissions found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getGroupPermissionById(Long groupPermissionId) {
        GroupPermission permission = groupPermissionRepository.findById(groupPermissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Group permission not found with id: " + groupPermissionId));
        return ResponseErrorTemplate.success("Group permission retrieved successfully", GroupPermissionMapper.toResponse(permission));
    }

    @Override
    public ResponseErrorTemplate createGroupPermission(Long groupId, Long funcId) {
        if (groupPermissionRepository.existsByGroupIdAndFuncId(groupId, funcId)) {
            throw new BadRequestException("Permission already exists for group " + groupId + " and function " + funcId);
        }
        GroupPermission saved = groupPermissionRepository.save(GroupPermissionMapper.toEntity(groupId, funcId));
        return ResponseErrorTemplate.success("Group permission created successfully", GroupPermissionMapper.toResponse(saved));
    }

    @Override
    public ResponseErrorTemplate updateGroupPermission(Long groupPermissionId, Boolean isActive) {
        GroupPermission permission = groupPermissionRepository.findById(groupPermissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Group permission not found with id: " + groupPermissionId));
        permission.setIsActive(isActive);
        GroupPermission updated = groupPermissionRepository.save(permission);
        return ResponseErrorTemplate.success("Group permission updated successfully", GroupPermissionMapper.toResponse(updated));
    }

    @Override
    public ResponseErrorTemplate deleteGroupPermission(Long groupPermissionId) {
        GroupPermission permission = groupPermissionRepository.findById(groupPermissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Group permission not found with id: " + groupPermissionId));
        groupPermissionRepository.delete(permission);
        return ResponseErrorTemplate.success("Group permission deleted successfully", null);
    }
}