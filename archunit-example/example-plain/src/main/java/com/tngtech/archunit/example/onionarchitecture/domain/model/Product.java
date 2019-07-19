package com.tngtech.archunit.example.onionarchitecture.domain.model;

import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ProductId;
import com.tngtech.archunit.example.onionarchitecture.domain.service.ProductName;

public class Product {
    // Dependency on ProductId violates the architecture, since ProductId resides with persistence adapter
    private final ProductId id;
    // Dependency on ProductName violates the architecture, since ProductName is located in the DomainService layer
    private final ProductName name;

    public Product(ProductId id, ProductName name) {
        if (id == null) {
            throw new IllegalArgumentException("Product id must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Product name must not be null");
        }
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", name=" + name + '}';
    }
}
