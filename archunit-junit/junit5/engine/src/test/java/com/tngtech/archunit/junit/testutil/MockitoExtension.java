package com.tngtech.archunit.junit.testutil;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.mockito.MockitoAnnotations;

// We can probably replace this, once Mockito 3.0 is out
public class MockitoExtension implements TestInstancePostProcessor {
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        MockitoAnnotations.initMocks(testInstance);
    }
}
