package com.tngtech.archunit.example.onionarchitecture.adapter.persistence;

import com.tngtech.archunit.example.onionarchitecture.domain.model.ShoppingCart;

@SuppressWarnings("unused")
public class ShoppingCartJpaRepository implements ShoppingCartRepository {
    @Override
    public ShoppingCart read(ShoppingCartId id) {
        // would normally load fully initialized shopping cart
        return new ShoppingCart(id);
    }

    @Override
    public void save(ShoppingCart shoppingCart) {
        // store shopping cart via JPA
    }
}
