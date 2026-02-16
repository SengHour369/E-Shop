//package com.example.learning_spring_security.ServiceMapper;
//
//import com.example.learning_spring_security.exception.ResourceNotFoundException;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import java.util.List;
//
//public interface BaseService<T, ID> {
//
//    T findById(ID id) throws ResourceNotFoundException;
//
//    List<T> findAll();
//
//    Page<T> findAll(Pageable pageable);
//
//    T save(T entity);
//
//    T update(ID id, T entity) throws ResourceNotFoundException;
//
//    void deleteById(ID id) throws ResourceNotFoundException;
//}