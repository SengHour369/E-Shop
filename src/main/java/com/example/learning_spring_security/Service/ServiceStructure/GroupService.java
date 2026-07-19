package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GroupRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;

public interface GroupService {

    @CacheEvict(value = "groups", allEntries = true)
    ResponseErrorTemplate createGroup(GroupRequest request);

    // Paginated – skip
    ResponseErrorTemplate getAllGroups(Pageable pageable);

    @Cacheable(value = "groups", key = "#id")
    ResponseErrorTemplate getGroupById(Long id);

    @CacheEvict(value = "groups", allEntries = true)
    ResponseErrorTemplate updateGroup(Long id, GroupRequest request);

    @CacheEvict(value = "groups", allEntries = true)
    ResponseErrorTemplate deleteGroup(Long id);

    @CacheEvict(value = "groups", key = "#id")
    ResponseErrorTemplate toggleGroupActive(Long id, Boolean isActive);
}