package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.Return;
import com.example.learning_spring_security.dto.Request.CancelRefundRequest;
import com.example.learning_spring_security.dto.Request.GetRefundListRequest;
import com.example.learning_spring_security.dto.Request.ProcessRefundRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface RefundService {

    @Cacheable(value = "refunds", key = "'summary'")
    ResponseErrorTemplate getRefundSummary();

    // Paginated – skip
    ResponseErrorTemplate getRefundList(GetRefundListRequest request);

    @Cacheable(value = "refunds", key = "#refundId")
    ResponseErrorTemplate getRefundDetail(String refundId);

    @Cacheable(value = "refunds", key = "#refundId + ':history'")
    ResponseErrorTemplate getRefundHistory(String refundId);

    @CacheEvict(value = "refunds", key = "#refundId")
    ResponseErrorTemplate processRefund(String refundId, ProcessRefundRequest request);

    @CacheEvict(value = "refunds", key = "#refundId")
    ResponseErrorTemplate cancelRefund(String refundId, CancelRefundRequest request);

    // Delegates to ProductService – caching handled there
    ResponseErrorTemplate getSimilarProducts(String refundId, Integer page, Integer size);

    // Called internally – no caching needed
    void createRefundFromReturn(Return returnRequest);
}