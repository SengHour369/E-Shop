package com.example.eshop.admin.service;

import com.example.eshop.admin.dto.StoreSettingsRequest;
import com.example.eshop.admin.model.StoreSettings;
import com.example.eshop.admin.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {
    private final StoreSettingsRepository repository;

    @Transactional(readOnly = true)
    public StoreSettingsRequest current() {
        return repository.findById(1L).map(this::response).orElse(null);
    }

    @Transactional
    public StoreSettingsRequest save(StoreSettingsRequest request) {
        var settings = repository.findById(1L).orElseGet(StoreSettings::new);
        settings.setId(1L);
        settings.setStoreName(request.storeName().trim());
        settings.setEmail(request.email().trim());
        settings.setPhone(request.phone());
        settings.setAddress(request.address());
        settings.setTimezone(request.timezone());
        return response(repository.save(settings));
    }

    private StoreSettingsRequest response(StoreSettings settings) {
        return new StoreSettingsRequest(settings.getStoreName(), settings.getEmail(),
                settings.getPhone(), settings.getAddress(), settings.getTimezone());
    }
}
