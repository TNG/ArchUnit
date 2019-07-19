package com.tngtech.archunit.example.onionarchitecture.adapter.persistence;

import java.util.UUID;

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
