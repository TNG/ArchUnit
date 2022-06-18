package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;

// Violates the architecture because Domain must be the owner of the interfaces, not the persistence adapter
@Adapter("persistence")
public interface ShoppingCartRepository {
    ShoppingCart read(ShoppingCartId id);

    void save(ShoppingCart shoppingCart);
}
