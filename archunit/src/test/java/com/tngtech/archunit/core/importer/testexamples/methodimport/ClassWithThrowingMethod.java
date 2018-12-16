package com.tngtech.archunit.core.importer.testexamples.methodimport;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ClassWithThrowingMethod {
    public String throwExceptions() throws IOException, InterruptedException {
        throw new IOException();
    }
}
