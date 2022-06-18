package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product;

import java.util.UUID;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;

@Adapter("persistence")
@SuppressWarnings("unused")
public class ProductId {
    private final UUID id;

    public ProductId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        this.id = id;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + '}';
    }
}
