//package com.example.learning_spring_security.Service.ServiceImplement;
//
//import com.example.learning_spring_security.Exception.ExceptionService.BadRequestException;
//import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
//import com.example.learning_spring_security.Model.FunctionPermission;
//import com.example.learning_spring_security.Repository.FunctionPermissionRepository;
//import com.example.learning_spring_security.Service.ServiceStructure.FunctionPermissionService;
//import com.example.learning_spring_security.ServiceMapper.FunctionPermissionMapper;
//import com.example.learning_spring_security.dto.Request.GetFunctionPermissionRequest;
//import com.example.learning_spring_security.dto.Response.FunctionPermissionPageResponse;
//import com.example.learning_spring_security.dto.Response.FunctionPermissionResponse;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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
//public class FunctionPermissionServiceImpl implements FunctionPermissionService {
//
//    private final FunctionPermissionRepository functionPermissionRepository;
//
//    @Override
//    @Transactional(readOnly = true)
//    public ResponseErrorTemplate getFunctions(GetFunctionPermissionRequest request) {
//        log.info("getFunctions: criteriaType={}, criteriaValue={}, page={}, size={}",
//                request.getCriteriaType(), request.getCriteriaValue(),
//                request.getPage(), request.getSize());
//
//        Pageable pageable = PageRequest.of(
//                request.getPage() - 1,
//                request.getSize(),
//                Sort.by("funcId").ascending()
//        );
//
//        Integer type = request.getCriteriaType();
//        String value = request.getCriteriaValue();
//        Page<FunctionPermission> page;
//        String successMsg;
//
//        if (type == null || type == 0 || value == null || value.isBlank()) {
//            page = functionPermissionRepository.findAll(pageable);
//            successMsg = "Retrieved all functions";
//        } else if (type == 1) { // filter by funcName (fuzzy)
//            page = functionPermissionRepository.findByFuncNameContaining(value, pageable);
//            successMsg = "Retrieved functions by name";
//        } else if (type == 2) { // filter by module
//            page = functionPermissionRepository.findByModule(value, pageable);
//            successMsg = "Retrieved functions by module";
//        } else if (type == 3) { // filter by isActive
//            Boolean isActive = Boolean.parseBoolean(value);
//            page = functionPermissionRepository.findByIsActive(isActive, pageable);
//            successMsg = "Retrieved functions by active status";
//        } else if (type == 4) { // filter by module + isActive, criteriaValue format: "module:true/false"
//            String[] parts = value.split(":");
//            if (parts.length != 2) {
//                throw new BadRequestException("criteriaValue for type 4 must be 'module:true/false'");
//            }
//            page = functionPermissionRepository.findByModuleAndIsActive(
//                    parts[0].trim(),
//                    Boolean.parseBoolean(parts[1].trim()),
//                    pageable
//            );
//            successMsg = "Retrieved functions by module and active status";
//        } else {
//            page = functionPermissionRepository.findAll(pageable);
//            successMsg = "Retrieved all functions";
//        }
//
//        List<FunctionPermissionResponse> payload = page.getContent()
//                .stream()
//                .map(FunctionPermissionMapper::toResponse)
//                .toList();
//
//        FunctionPermissionPageResponse pageResponse = FunctionPermissionPageResponse.builder()
//                .payload(payload)
//                .totalItems(page.getTotalElements())
//                .totalPages(page.getTotalPages())
//                .currentPage(page.getNumber() + 1)
//                .pageSize(page.getSize())
//                .build();
//
//        String message = page.isEmpty() ? "No functions found" : successMsg;
//        return ResponseErrorTemplate.success(message, pageResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ResponseErrorTemplate getFunctionById(Long funcId) {
//        FunctionPermission function = functionPermissionRepository.findById(funcId)
//                .orElseThrow(() -> new ResourceNotFoundException("Function not found with id: " + funcId));
//        return ResponseErrorTemplate.success("Function retrieved successfully",
//                FunctionPermissionMapper.toResponse(function));
//    }
//
//    @Override
//    public ResponseErrorTemplate createFunction(String funcCode, String funcName, String description, String module) {
//        if (functionPermissionRepository.existsByFuncCode(funcCode)) {
//            throw new BadRequestException("Function with code '" + funcCode + "' already exists");
//        }
//
//        FunctionPermission saved = functionPermissionRepository.save(
//                FunctionPermissionMapper.toEntity(funcCode, funcName, description, module));
//        return ResponseErrorTemplate.success("Function created successfully",
//                FunctionPermissionMapper.toResponse(saved));
//    }
//
//    @Override
//    public ResponseErrorTemplate updateFunction(Long funcId, String funcName, String description, Boolean isActive) {
//        FunctionPermission function = functionPermissionRepository.findById(funcId)
//                .orElseThrow(() -> new ResourceNotFoundException("Function not found with id: " + funcId));
//
//        if (funcName != null && !funcName.isBlank()) function.setFuncName(funcName);
//        if (description != null) function.setDescription(description);
//        if (isActive != null) function.setIsActive(isActive);
//
//        FunctionPermission updated = functionPermissionRepository.save(function);
//        return ResponseErrorTemplate.success("Function updated successfully",
//                FunctionPermissionMapper.toResponse(updated));
//    }
//
//    @Override
//    public ResponseErrorTemplate deleteFunction(Long funcId) {
//        FunctionPermission function = functionPermissionRepository.findById(funcId)
//                .orElseThrow(() -> new ResourceNotFoundException("Function not found with id: " + funcId));
//        functionPermissionRepository.delete(function);
//        return ResponseErrorTemplate.success("Function deleted successfully", null);
//    }
//}