package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.service.impl.ProductResponseService;
import com.example.eshop.common.security.LivePermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequiredArgsConstructor
public class SalesCategoryController {
 private final ProductSkuRepository skus;
 private final ProductResponseService responses;
 private final LivePermissionService permissions;
 @PostMapping("/api/v1/products/reports/sku-categories") @Transactional(readOnly=true)
 public Map<String,String> categories(@Valid @RequestBody @Size(max=500) List<@NotNull @Positive Long> ids){
  permissions.require("REPORT_VIEW");
  var rows=skus.findAllById(ids);var labels=responses.aiCategoryNames(rows.stream().map(s->s.getProduct().getId()).distinct().toList());
  Map<String,String> result=new LinkedHashMap<>();for(var row:rows)result.put(row.getId().toString(),labels.getOrDefault(row.getProduct().getId(),"Uncategorized"));return result;
 }
}
