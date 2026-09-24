package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GroupRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Pageable;

public interface GroupService {

    ResponseErrorTemplate createGroup(GroupRequest request);

    ResponseErrorTemplate getAllGroups(Pageable pageable);

    ResponseErrorTemplate getGroupById(Long id);

    ResponseErrorTemplate updateGroup(Long id, GroupRequest request);

    ResponseErrorTemplate deleteGroup(Long id);

    ResponseErrorTemplate toggleGroupActive(Long id, Boolean isActive);
}