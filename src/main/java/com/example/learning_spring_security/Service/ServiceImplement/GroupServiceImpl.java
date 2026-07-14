package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Group;
import com.example.learning_spring_security.Repository.GroupRepository;
import com.example.learning_spring_security.Service.ServiceStructure.GroupService;
import com.example.learning_spring_security.ServiceMapper.GroupMapper;
import com.example.learning_spring_security.dto.Request.GroupRequest;
import com.example.learning_spring_security.dto.Response.GroupResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;

    // Helper: generate unique group code from name
    private String generateUniqueGroupCode(String name) {
        String base = name.toUpperCase().replaceAll("\\s+", "");
        if (base.length() > 10) {
            base = base.substring(0, 10);
        }
        if (base.isEmpty()) {
            base = "GROUP";
        }
        String candidate = base;
        int suffix = 1;
        while (groupRepository.existsByGroupCode(candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    @Override
    @Transactional
    public ResponseErrorTemplate createGroup(GroupRequest request) {
        log.info("Creating group with name: {}", request.getName());

        Group group = GroupMapper.toEntity(
                generateUniqueGroupCode(request.getName()),
                request.getName(),
                request.getDescription(),
                request.getStatus(),
                request.getType()
        );
        group.setIsActive(request.getIsActive() != null && request.getIsActive());

        Group saved = groupRepository.save(group);
        log.info("Group created with id: {}", saved.getId());

        return ResponseErrorTemplate.success("Group created successfully", GroupMapper.toResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getAllGroups(Pageable pageable) {
        Page<Group> page = groupRepository.findAll(pageable);
        Page<GroupResponse> responsePage = page.map(GroupMapper::toResponse);
        String message = page.isEmpty() ? "No groups found" : "Groups retrieved successfully";
        return ResponseErrorTemplate.success(message, responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getGroupById(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));
        return ResponseErrorTemplate.success("Group retrieved successfully", GroupMapper.toResponse(group));
    }

    @Override
    @Transactional
    public ResponseErrorTemplate updateGroup(Long id, GroupRequest request) {
        log.info("Updating group with id: {}", id);

        Group existing = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setStatus(request.getStatus());
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }

        Group updated = groupRepository.save(existing);
        log.info("Group updated with id: {}", updated.getId());

        return ResponseErrorTemplate.success("Group updated successfully", GroupMapper.toResponse(updated));
    }

    @Override
    @Transactional
    public ResponseErrorTemplate deleteGroup(Long id) {
        log.info("Soft-deleting group with id: {}", id);

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));

        group.setIsDelete(true);
        group.setIsActive(false);
        groupRepository.save(group);

        log.info("Group soft-deleted with id: {}", id);
        return ResponseErrorTemplate.success("Group deleted successfully", null);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate toggleGroupActive(Long id, Boolean isActive) {
        log.info("Toggling active status for group id {} to {}", id, isActive);

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));

        group.setIsActive(isActive != null && isActive);
        groupRepository.save(group);

        log.info("Group active status updated for id: {}", id);
        return ResponseErrorTemplate.success("Group active status updated", GroupMapper.toResponse(group));
    }
}