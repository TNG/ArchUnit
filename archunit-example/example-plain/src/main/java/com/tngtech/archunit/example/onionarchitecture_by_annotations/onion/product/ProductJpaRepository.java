package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;

@Adapter("persistence")
@SuppressWarnings("unused")
public class ProductJpaRepository implements ProductRepository {
    @Override
    public Product read(ProductId id) {
        return new Product(id, new ProductName("would normally be read"));
    }

    @Override
    public long getTotalCount() {
        return 0;
    }
}
