package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.GroupRequest;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;
import org.springframework.data.domain.Pageable;

public interface GroupService {

    ResponseErrorTemplate createGroup(GroupRequest request);

    ResponseErrorTemplate getAllGroups(Pageable pageable);

    ResponseErrorTemplate getGroupById(Long id);

    ResponseErrorTemplate updateGroup(Long id, GroupRequest request);

    ResponseErrorTemplate deleteGroup(Long id);

    ResponseErrorTemplate toggleGroupActive(Long id, Boolean isActive);
}