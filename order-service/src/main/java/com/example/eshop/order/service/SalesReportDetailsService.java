package com.example.eshop.order.service;
import com.example.eshop.order.repository.OrderItemRepository;
import com.example.eshop.order.client.CatalogCheckoutClient;
import com.example.eshop.order.dto.response.MonthlyItemsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
@Service @RequiredArgsConstructor
public class SalesReportDetailsService {
 private final OrderItemRepository items;
 private final CatalogCheckoutClient catalog;
 public record Category(String name,BigDecimal amount,long items){}
 public record Details(List<MonthlyItemsResponse> monthlyItems,List<Category> categories,boolean categoriesAvailable,String categoryBasis){}
 @Transactional(readOnly=true)
 public Details yearly(int year,String bearer){
  var start=java.time.LocalDate.of(year,1,1).atStartOfDay();var end=start.plusYears(1);
  var monthly=items.monthlyItems(start,end);var sales=items.salesBySku(start,end);
  var ids=sales.stream().map(s->s.skuId()).toList();Map<String,String> labels=new HashMap<>();boolean available=true;
  try{for(int i=0;i<ids.size();i+=500)labels.putAll(catalog.salesCategories(bearer,ids.subList(i,Math.min(i+500,ids.size()))));}
  catch(Exception unavailable){available=false;}
  Map<String,Category> grouped=new TreeMap<>();
  if(available)for(var row:sales){String name=labels.getOrDefault(row.skuId().toString(),"Uncategorized");var previous=grouped.getOrDefault(name,new Category(name,BigDecimal.ZERO,0));grouped.put(name,new Category(name,previous.amount().add(row.amount()),previous.items()+row.items()));}
  return new Details(monthly,new ArrayList<>(grouped.values()),available,"Recorded item sales, grouped by current catalog category; excludes order-level fees.");
 }
}
