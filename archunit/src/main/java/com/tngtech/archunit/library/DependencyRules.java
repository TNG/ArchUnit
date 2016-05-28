package com.tngtech.archunit.library;

import com.tngtech.archunit.lang.AbstractArchCondition;
import com.tngtech.archunit.library.slices.Slice;
import com.tngtech.archunit.library.slices.SlicesArchCondition;

public class DependencyRules {
    public static AbstractArchCondition<Slice> noCycles() {
        return new SlicesArchCondition();
    }
}
