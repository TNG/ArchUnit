package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;

// Violates the architecture because Domain must be the owner of the interfaces, not the persistence adapter
@Adapter("persistence")
public interface ProductRepository {
    Product read(ProductId id);

    long getTotalCount();
}
