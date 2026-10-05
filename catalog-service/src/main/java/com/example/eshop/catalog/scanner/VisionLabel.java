package com.example.eshop.catalog.scanner;

/** A name suggested by image recognition. It is not a catalog id. */
public record VisionLabel(String name, double confidence) {}
