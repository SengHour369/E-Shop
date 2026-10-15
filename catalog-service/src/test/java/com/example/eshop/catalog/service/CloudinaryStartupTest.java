package com.example.eshop.catalog.service;

import com.cloudinary.Cloudinary;
import com.example.eshop.catalog.config.CloudinaryConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryStartupTest {
    @Test
    void catalogImageClientStartsWithoutExternalCredentials() {
        new ApplicationContextRunner()
                .withInitializer(context -> {
                    try {
                        var sources = new YamlPropertySourceLoader().load("catalog", new ClassPathResource("application.yml"));
                        sources.forEach(context.getEnvironment().getPropertySources()::addLast);
                    } catch (java.io.IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                })
                .withUserConfiguration(CloudinaryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(Cloudinary.class);
                });
    }
}
