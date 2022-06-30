package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.DomainService;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.order.OrderItem;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.order.OrderQuantity;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.Product;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.ProductId;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.ProductRepository;

@DomainService
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
