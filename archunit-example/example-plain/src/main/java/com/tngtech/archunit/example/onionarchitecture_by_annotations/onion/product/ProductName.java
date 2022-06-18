package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.DomainService;

@DomainService
@SuppressWarnings("unused")
public class ProductName {
    private final String name;

    public ProductName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        this.name = name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='" + name + '\'' + '}';
    }
}
