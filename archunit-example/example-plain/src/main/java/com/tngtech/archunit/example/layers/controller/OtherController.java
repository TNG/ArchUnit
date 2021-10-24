package com.tngtech.archunit.example.layers.controller;

import com.tngtech.archunit.example.layers.controller.marshaller.StringUnmarshaller;

@SuppressWarnings("unused")
public class OtherController {
    void receive(@UnmarshalTransport(StringUnmarshaller.class) Object param) {
    }
}
