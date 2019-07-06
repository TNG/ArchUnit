package com.tngtech.archunit.example.layers.service;

import com.tngtech.archunit.example.layers.security.Secured;

/**
 * Well modelled code always has lots of 'helpers' ;-)
 */
public class ServiceHelper {
    public Object insecure = new Object();
    @Secured
    public Object properlySecured = new Object();

    public ServiceHelper() {
    }

    @Secured
    public ServiceHelper(String properlySecured) {
    }
}
