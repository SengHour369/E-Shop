package com.example.learning_spring_security.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public abstract class BaseController {

    protected Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        return PageRequest.of(page, size, sort);
    }

    protected Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by("id").descending());
    }
}