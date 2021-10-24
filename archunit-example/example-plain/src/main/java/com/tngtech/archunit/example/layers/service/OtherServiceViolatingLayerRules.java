package com.tngtech.archunit.example.layers.service;

import com.tngtech.archunit.example.layers.controller.UnmarshalTransport;
import com.tngtech.archunit.example.layers.controller.marshaller.ByteUnmarshaller;
import com.tngtech.archunit.example.layers.controller.marshaller.StringUnmarshaller;

public class OtherServiceViolatingLayerRules {
    @SuppressWarnings("unused")
    public void dependentOnParameterAnnotation(@UnmarshalTransport({StringUnmarshaller.class, ByteUnmarshaller.class}) Object param) {
    }
}
