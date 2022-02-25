package com.tngtech.archunit.core.importer.testexamples.trycatch;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

@SuppressWarnings("unused")
public class ClassWithTryWithResources {
    void method() {
        try (
                ByteArrayInputStream one = new ByteArrayInputStream(new byte[0]);
                FileInputStream two = new FileInputStream("any")
        ) {
            new Object();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
