package com.tngtech.archunit.library;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.library.slices.Slice;
import com.tngtech.archunit.library.slices.SlicesArchCondition;

public class DependencyRules {
    public static ArchCondition<Slice> noCycles() {
        return new SlicesArchCondition();
    }
}
