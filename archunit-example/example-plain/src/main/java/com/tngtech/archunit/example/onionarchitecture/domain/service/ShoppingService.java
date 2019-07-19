package com.tngtech.archunit.example.onionarchitecture.domain.service;

import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ProductId;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ProductRepository;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ShoppingCartId;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ShoppingCartRepository;
import com.tngtech.archunit.example.onionarchitecture.domain.model.OrderItem;
import com.tngtech.archunit.example.onionarchitecture.domain.model.Product;
import com.tngtech.archunit.example.onionarchitecture.domain.model.ShoppingCart;

public class ShoppingService {
    private final ShoppingCartRepository shoppingCartRepository;
    private final ProductRepository productRepository;

    public ShoppingService(ShoppingCartRepository shoppingCartRepository, ProductRepository productRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.productRepository = productRepository;
    }

    public void addToShoppingCart(ShoppingCartId shoppingCartId, ProductId productId, OrderQuantity quantity) {
        ShoppingCart shoppingCart = shoppingCartRepository.read(shoppingCartId);
        Product product = productRepository.read(productId);
        OrderItem newItem = new OrderItem(product, quantity);
        shoppingCart.add(newItem);
        shoppingCartRepository.save(shoppingCart);
    }
}
