package com.tngtech.archunit.example.layers.service;

import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.example.layers.controller.SomeUtility;
import com.tngtech.archunit.example.layers.controller.one.SomeEnum;
import com.tngtech.archunit.example.layers.security.Secured;

/**
 * Well modelled code always has lots of 'helpers' ;-)
 */
@SuppressWarnings("unused")
public class ServiceHelper<
        TYPE_PARAMETER_VIOLATING_LAYER_RULE extends SomeUtility,
        ANOTHER_TYPE_PARAMETER_VIOLATING_LAYER_RULE extends Map<?, Set<? super SomeEnum>>> {

    public Object insecure = new Object();
    @Secured
    public Object properlySecured = new Object();

    public ServiceHelper() {
    }

    @Secured
    public ServiceHelper(String properlySecured) {
    }

    static <
            METHOD_TYPE_PARAMETER_VIOLATING_LAYER_RULE extends SomeUtility,
            ANOTHER_METHOD_TYPE_PARAMETER_VIOLATING_LAYER_RULE extends Map<?, Set<? super SomeEnum>>>
    ServiceHelper<METHOD_TYPE_PARAMETER_VIOLATING_LAYER_RULE, ANOTHER_METHOD_TYPE_PARAMETER_VIOLATING_LAYER_RULE> violatingLayerRuleByMethodTypeParameters() {
        return new ServiceHelper<>();
    }
}
