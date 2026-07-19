package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AddressService {

    @CacheEvict(value = "addresses", key = "#userId + ':list'")
    ResponseErrorTemplate createAddress(AddressRequest request, Long userId);

    @Cacheable(value = "addresses", key = "#id")
    ResponseErrorTemplate getAddressById(Long id);

    // Paginated – skip caching
    Page<ResponseErrorTemplate> getUserAddresses(Long userId, Pageable pageable);

    @Cacheable(value = "addresses", key = "#userId + ':list'")
    List<ResponseErrorTemplate> getUserAddresses(Long userId);

    @Caching(evict = {
            @CacheEvict(value = "addresses", key = "#id"),
            @CacheEvict(value = "addresses", key = "#userId + ':list'")
    })
    ResponseErrorTemplate updateAddress(Long id, AddressRequest request, Long userId);

    @Caching(evict = {
            @CacheEvict(value = "addresses", key = "#id"),
            @CacheEvict(value = "addresses", key = "#userId + ':list'")
    })
    void deleteAddress(Long id, Long userId);

    @Caching(evict = {
            @CacheEvict(value = "addresses", key = "#addressId"),
            @CacheEvict(value = "addresses", key = "#userId + ':list'"),
            @CacheEvict(value = "addresses", key = "#userId + ':default'")
    })
    ResponseErrorTemplate setDefaultAddress(Long addressId, Long userId);

    @Cacheable(value = "addresses", key = "#userId + ':default'")
    ResponseErrorTemplate getDefaultAddress(Long userId);
}