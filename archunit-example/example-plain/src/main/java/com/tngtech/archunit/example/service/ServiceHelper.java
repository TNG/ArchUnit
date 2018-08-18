package com.tngtech.archunit.example.service;

import com.tngtech.archunit.example.security.Secured;

/**
 * Well modelled code always has lots of 'helpers' ;-)
 */
public class ServiceHelper {
    public ServiceHelper() {
    }

    @Secured
    public ServiceHelper(String properlySecured) {
    }
}
