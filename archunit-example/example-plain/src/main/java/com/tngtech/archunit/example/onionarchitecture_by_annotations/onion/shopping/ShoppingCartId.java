package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping;

import java.util.UUID;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;

@Adapter("persistence")
public class ShoppingCartId {
    private final UUID id;

    public ShoppingCartId(UUID id) {
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
