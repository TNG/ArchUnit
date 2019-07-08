package com.tngtech.archunit.example.onionarchitecture.domain.service;

@SuppressWarnings("unused")
public class ProductName {
    private final String name;

    public ProductName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        this.name = name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='" + name + '\'' + '}';
    }
}
