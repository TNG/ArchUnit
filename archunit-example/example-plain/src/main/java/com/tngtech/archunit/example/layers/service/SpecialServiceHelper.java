package com.tngtech.archunit.example.layers.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.example.layers.controller.SomeUtility;
import com.tngtech.archunit.example.layers.controller.one.SomeEnum;

@SuppressWarnings("unused")
public abstract class SpecialServiceHelper extends ServiceHelper<SomeUtility, HashMap<?, Set<? super SomeEnum>>> implements List<Set<? super SomeUtility>> {
    Map<?, Map<SomeEnum, ? extends SomeUtility>> fieldWithGenericTypeViolatingLayerRule;

    Map<?, Map<SomeEnum, ? extends SomeUtility>> methodWithGenericReturnTypeViolatingLayerRule() {
        return null;
    }
}
