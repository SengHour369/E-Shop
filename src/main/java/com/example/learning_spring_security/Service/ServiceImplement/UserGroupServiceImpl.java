//package com.example.learning_spring_security.Service.ServiceImplement;
//
//import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
//import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
//import com.example.learning_spring_security.Model.UserGroup;
//import com.example.learning_spring_security.Repository.UserGroupRepository;
//import com.example.learning_spring_security.Service.ServiceStructure.UserGroupService;
//import com.example.learning_spring_security.ServiceMapper.UserGroupMapper;
//import com.example.learning_spring_security.dto.Request.GetUserGroupRequest;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
//import com.example.learning_spring_security.dto.Response.UserGroupPageResponse;
//import com.example.learning_spring_security.dto.Response.UserGroupResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class UserGroupServiceImpl implements UserGroupService {
//
//    private final UserGroupRepository userGroupRepository;
//
//    @Override
//    @Transactional(readOnly = true)
//    public ResponseErrorTemplate getUserGroups(GetUserGroupRequest request) {
//        log.info("getUserGroups called: criteriaType={}, criteriaValue={}, page={}, size={}",
//                request.getCriteriaType(), request.getCriteriaValue(),
//                request.getPage(), request.getSize());
//
//        Pageable pageable = PageRequest.of(
//                request.getPage() - 1,
//                request.getSize(),
//                Sort.by("groupId").descending()
//        );
//
//        Integer type = request.getCriteriaType();
//        String value = request.getCriteriaValue();
//
//        Page<UserGroup> page;
//        String successMsg;
//
//        if (type == null || type == 0 || value == null || value.isBlank()) {
//            page = userGroupRepository.findAllActive(pageable);
//            successMsg = "Retrieved all groups";
//        } else if (type == 1) {
//            page = userGroupRepository.findAllByGroupCode(value, pageable);
//            successMsg = "Retrieved groups by code";
//        } else if (type == 2) {
//            page = userGroupRepository.findAllByGroupNameFuzzy(value, pageable);
//            successMsg = "Retrieved groups by name";
//        } else if (type == 3) {
//            Boolean isActive = Boolean.parseBoolean(value);
//            page = userGroupRepository.findAllByIsActive(isActive, pageable);
//            successMsg = "Retrieved groups by active status";
//        } else if (type == 4) {
//            page = userGroupRepository.findAllByDisplayFuzzy(value, pageable);
//            successMsg = "Retrieved groups by display";
//        } else {
//            page = userGroupRepository.findAllActive(pageable);
//            successMsg = "Retrieved all groups";
//        }
//
//        List<UserGroupResponse> payload = page.getContent()
//                .stream()
//                .map(UserGroupMapper::toResponse)
//                .toList();
//
//        UserGroupPageResponse pageResponse = UserGroupPageResponse.builder()
//                .payload(payload)
//                .totalItems(page.getTotalElements())
//                .totalPages(page.getTotalPages())
//                .currentPage(page.getNumber() + 1)
//                .pageSize(page.getSize())
//                .build();
//
//        log.info("getUserGroups completed: totalItems={}, totalPages={}", page.getTotalElements(), page.getTotalPages());
//
//        String message = page.isEmpty() ? "No groups found" : successMsg;
//        return ResponseErrorTemplate.success(message, pageResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ResponseErrorTemplate getUserGroupById(Long groupId) {
//        UserGroup group = userGroupRepository.findById(groupId)
//                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
//        return ResponseErrorTemplate.success("Group retrieved successfully", UserGroupMapper.toResponse(group));
//    }
//
//    @Override
//    public ResponseErrorTemplate createUserGroup(String groupCode, String groupName, String display) {
//        if (userGroupRepository.existsByGroupCode(groupCode)) {
//            throw new BadRequestException("Group already exists with code: " + groupCode);
//        }
//       // UserGroup saved = userGroupRepository.save(UserGroupMapper.toEntity(groupCode, groupName, display));
////        return ResponseErrorTemplate.success("Group created successfully", UserGroupMapper.toResponse(saved));
//        return null;
//    }
//
//    @Override
//    public ResponseErrorTemplate updateUserGroup(Long groupId, String groupName, String display, Boolean isActive) {
//        UserGroup group = userGroupRepository.findById(groupId)
//                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
//
////        if (groupName != null && !groupName.isBlank()) group.setGroupName(groupName);
////        if (display != null && !display.isBlank()) group.setDisplay(display);
//        if (isActive != null) group.setIsActive(isActive);
//
//        UserGroup updated = userGroupRepository.save(group);
//        return ResponseErrorTemplate.success("Group updated successfully", UserGroupMapper.toResponse(updated));
//    }
//
//    @Override
//    public ResponseErrorTemplate deleteUserGroup(Long groupId) {
//        UserGroup group = userGroupRepository.findById(groupId)
//                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
//        group.setIsDelete(true);
//        group.setIsActive(false);
//        userGroupRepository.save(group);
//        return ResponseErrorTemplate.success("Group deleted successfully", null);
//    }
//}