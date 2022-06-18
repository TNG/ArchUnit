package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping;

import java.util.UUID;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.order.OrderQuantity;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.ProductId;

@Adapter("rest")
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
