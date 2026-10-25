package com.example.eshop.order.repository;

import com.example.eshop.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderDetailId(Long orderId);


    @org.springframework.data.jpa.repository.Query("""
      select new com.example.eshop.order.dto.response.SkuSalesResponse(i.productSkuId,sum(i.quantity),sum(i.totalPrice))
      from OrderItem i join i.orderDetail o
      where (o.deleted=false or o.deleted is null) and o.orderDate>=:start and o.orderDate<:end
       and o.status not in ('CANCELLED','FAILED','REFUNDED','CHECKOUT_PENDING')
      group by i.productSkuId
      """)
    List<com.example.eshop.order.dto.response.SkuSalesResponse> salesBySku(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,@org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
    @org.springframework.data.jpa.repository.Query("""
      select new com.example.eshop.order.dto.response.MonthlyItemsResponse(month(o.orderDate),sum(i.quantity))
      from OrderItem i join i.orderDetail o
      where (o.deleted=false or o.deleted is null) and o.orderDate>=:start and o.orderDate<:end
       and o.status not in ('CANCELLED','FAILED','REFUNDED','CHECKOUT_PENDING')
      group by month(o.orderDate) order by month(o.orderDate)
      """)
    List<com.example.eshop.order.dto.response.MonthlyItemsResponse> monthlyItems(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,@org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
}
