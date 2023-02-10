package com.tngtech.archunit.example.singleton;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Per Lundberg
 */
public class SingletonClass {

    // Poor man's reimplementation of Guava's memoization support. If your project supports Guava,
    // replace the stuff below with the following:
    //
    // private static final Supplier<SingletonClass> INSTANCE = Suppliers.memoize( SingletonClass::new );
    //
    //
    // ...and retrieve the (singleton) instance by calling `INSTANCE.get()`
    //
    private static final ConcurrentHashMap<Integer, SingletonClass> INSTANCE_MAP = new ConcurrentHashMap<>();
    private static final int INSTANCE_KEY = 1;


    private SingletonClass() {
        // Private constructor to prevent construction
    }

    public void doSomething() {
        throw new UnsupportedOperationException();
    }

    public static SingletonClass getInstance() {
        return INSTANCE_MAP.computeIfAbsent( INSTANCE_KEY, unused -> new SingletonClass() );
    }
}
