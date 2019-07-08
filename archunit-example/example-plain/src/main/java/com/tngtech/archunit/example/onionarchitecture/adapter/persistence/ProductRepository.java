package com.tngtech.archunit.example.onionarchitecture.adapter.persistence;

import com.tngtech.archunit.example.onionarchitecture.domain.model.Product;

// Violates the architecture because Domain must be the owner of the interfaces, not the persistence adapter
public interface ProductRepository {
    Product read(ProductId id);

    long getTotalCount();
}
