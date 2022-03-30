package com.tngtech.archunit.core.domain.testexamples;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class AReferencingB {
    void referenceConstructors() {
        Supplier<BReferencedByA> noArgs = BReferencedByA::new;
        Function<String, BReferencedByA> oneArg = BReferencedByA::new;
    }

    void referenceMethods(BReferencedByA b) {
        Supplier<String> getSomeField = b::getSomeField;
        Function<BReferencedByA, String> getSomeFieldStatically = BReferencedByA::getSomeField;
        Runnable runnable = b::getNothing;
    }
}
