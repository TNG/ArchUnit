package com.tngtech.archunit.example.layers.controller.marshaller;

public interface Unmarshaller<F> {
    @SuppressWarnings("unused")
    <T> T unmarschal(F from);
}
