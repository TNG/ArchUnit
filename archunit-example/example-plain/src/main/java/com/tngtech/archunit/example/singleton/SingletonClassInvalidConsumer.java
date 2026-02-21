package com.tngtech.archunit.example.singleton;

/**
 * An example of how {@link SingletonClass} is used incorrectly. This is expected to be detected by
 * the corresponding ArchUnit test.
 *
 * @author Per Lundberg
 */
public class SingletonClassInvalidConsumer {

    void doSomething() {
        // This pattern (of calling getInstance() in the midst of a method) is both unadvisable and
        // dangerous for the following reasons:
        //
        // - It makes it impossible to override the dependency for tests, which can lead to
        //   unnecessarily excessive object mocking.
        //
        // - It postpones object initialization to an unnecessarily late stage (runtime instead of
        //   startup-time). Problems with classes that cannot be instantiated risks being "hidden"
        //   until runtime, defeating the purpose of the "fail fast" philosophy.
        SingletonClass.getInstance().doSomething();
    }
}
