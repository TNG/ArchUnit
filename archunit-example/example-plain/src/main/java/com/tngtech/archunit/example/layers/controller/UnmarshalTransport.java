package com.tngtech.archunit.example.layers.controller;

import java.lang.annotation.Retention;

import com.tngtech.archunit.example.layers.controller.marshaller.Unmarshaller;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface UnmarshalTransport {
    Class<? extends Unmarshaller<?>>[] value();
}
