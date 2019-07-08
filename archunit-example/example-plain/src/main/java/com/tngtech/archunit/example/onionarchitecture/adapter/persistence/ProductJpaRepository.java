package com.tngtech.archunit.example.onionarchitecture.adapter.persistence;

import com.tngtech.archunit.example.onionarchitecture.domain.model.Product;
import com.tngtech.archunit.example.onionarchitecture.domain.service.ProductName;

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
