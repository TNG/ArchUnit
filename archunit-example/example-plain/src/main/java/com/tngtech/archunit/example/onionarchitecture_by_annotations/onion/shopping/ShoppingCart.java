package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.shopping;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.DomainModel;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.order.OrderItem;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.order.PaymentMethod;

@DomainModel
@SuppressWarnings("unused")
public class ShoppingCart {
    private final ShoppingCartId id;
    private final Set<OrderItem> orderItems = new HashSet<>();

    public ShoppingCart(ShoppingCartId id) {
        this.id = id;
    }

    public void add(OrderItem orderItem) {
        orderItems.add(orderItem);
    }

    public void executeOrder(PaymentMethod method) {
        // complete financial transaction and initiate shipping process
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", orderItems=" + orderItems + '}';
    }
}
