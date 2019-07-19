package com.tngtech.archunit.example.onionarchitecture.adapter.rest;

import java.util.UUID;

import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ProductId;
import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ShoppingCartId;
import com.tngtech.archunit.example.onionarchitecture.domain.service.OrderQuantity;
import com.tngtech.archunit.example.onionarchitecture.domain.service.ShoppingService;

@SuppressWarnings("unused")
public class ShoppingController {
    private final ShoppingService shoppingService;

    public ShoppingController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    // @POST or similar
    public void addToShoppingCart(UUID shoppingCartId, UUID productId, int quantity) {
        shoppingService.addToShoppingCart(new ShoppingCartId(shoppingCartId), new ProductId(productId), new OrderQuantity(quantity));
    }
}
