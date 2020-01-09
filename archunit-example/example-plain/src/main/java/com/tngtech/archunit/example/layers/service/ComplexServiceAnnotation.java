package com.tngtech.archunit.example.layers.service;

import com.tngtech.archunit.example.layers.controller.ComplexControllerAnnotation;
import com.tngtech.archunit.example.layers.controller.one.SomeEnum;

public @interface ComplexServiceAnnotation {
    ComplexControllerAnnotation controllerAnnotation();

    SimpleServiceAnnotation simpleServiceAnnotation();

    SomeEnum controllerEnum();

    ServiceType serviceType();
}
