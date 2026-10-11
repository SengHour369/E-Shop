package com.example.eshop.catalog.seed;

import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Version-controlled starter taxonomy. Images are bundled and never fetched from external sites. */
public final class CatalogSeedData {
    private CatalogSeedData() {}
    public record CategorySeed(String name, String group, String slug) {}
    public record BrandSeed(String name, String slug) {}
    public static List<CategorySeed> categories() {
        return lines("categories").stream().map(line -> {
            String[] fields = line.split("\t");
            return new CategorySeed(fields[0], fields[1], fields[2]);
        }).toList();
    }
    public static List<BrandSeed> brands() {
        return lines("brands").stream().map(line -> {
            String[] fields = line.split("\t");
            return new BrandSeed(fields[0], fields[1]);
        }).toList();
    }
    private static List<String> lines(String name) {
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("seed/" + name + ".tsv").getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().filter(line -> !line.isBlank()).toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot load catalog seed " + name, ex);
        }
    }
}
