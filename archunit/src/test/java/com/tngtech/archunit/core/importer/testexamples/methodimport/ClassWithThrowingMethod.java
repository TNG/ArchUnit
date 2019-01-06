package com.tngtech.archunit.core.importer.testexamples.methodimport;

import com.tngtech.archunit.core.importer.testexamples.FirstCheckedException;
import com.tngtech.archunit.core.importer.testexamples.SecondCheckedException;

@SuppressWarnings("unused")
public class ClassWithThrowingMethod {
    public String throwExceptions() throws FirstCheckedException, SecondCheckedException {
        if (Math.random() > 0.5) {
            throw new FirstCheckedException();
        } else {
            throw new SecondCheckedException();
        }
    }
}
