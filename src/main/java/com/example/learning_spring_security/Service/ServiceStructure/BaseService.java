package com.example.learning_spring_security.Service.ServiceStructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BaseService<T, ID> {
    T findById(ID id);
    List<T> findAll();
    Page<T> findAll(Pageable pageable);
    T save(T entity);
    T update(ID id, T entity);
    void deleteById(ID id);
}