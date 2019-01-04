package com.tngtech.archunit.core.importer.testexamples.constructorimport;

import com.tngtech.archunit.core.importer.testexamples.FirstCheckedException;
import com.tngtech.archunit.core.importer.testexamples.SecondCheckedException;

public class ClassWithThrowingConstructor {
    public ClassWithThrowingConstructor() throws FirstCheckedException, SecondCheckedException {
        if (Math.random() > 0.5) {
            throw new FirstCheckedException();
        } else {
            throw new SecondCheckedException();
        }
    }
}
