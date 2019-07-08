package com.tngtech.archunit.example.onionarchitecture.adapter.persistence;

import com.tngtech.archunit.example.onionarchitecture.domain.model.ShoppingCart;

// Violates the architecture because Domain must be the owner of the interfaces, not the persistence adapter
public interface ShoppingCartRepository {
    ShoppingCart read(ShoppingCartId id);

    void save(ShoppingCart shoppingCart);
}
