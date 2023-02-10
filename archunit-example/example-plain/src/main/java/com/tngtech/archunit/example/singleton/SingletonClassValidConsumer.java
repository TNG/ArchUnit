package com.tngtech.archunit.example.singleton;

/**
 * An example of how {@link SingletonClass} is used correctly
 *
 * @author Per Lundberg
 */
public class SingletonClassValidConsumer {

    private static SingletonClassValidConsumer instance;

    private final SingletonClass singletonClass;

    public SingletonClassValidConsumer( SingletonClass singletonClass ) {
        this.singletonClass = singletonClass;
    }

    public static SingletonClassValidConsumer getInstance() {
        // Valid way to call getInstance() on another class:
        //
        // - We retrieve the instance for the dependency in our own getInstance() method. It is
        //   passed to our constructor as a constructor parameter, making it easy to override it for
        //   tests.
        if ( instance == null ) {
            instance = new SingletonClassValidConsumer( SingletonClass.getInstance() );
        }

        return instance;
    }
}
