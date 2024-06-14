package com.tngtech.archunit.example.singleton;

/**
 * An example of how {@link SingletonClass} is used incorrectly, but explicitly whitelisted.
 *
 * @author Per Lundberg
 */
public class SingletonClassWhitelistedInvalidConsumer {

    void doSomething() {
        // This pattern (of calling getInstance() in the midst of a method) is both unadvisable and
        // dangerous for reasons described in SingleTonClassInvalidConsumer.
        SingletonClass.getInstance().doSomething();
    }
}
