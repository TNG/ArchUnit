package com.tngtech.archunit.junit.internal.testutil;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TestLogExtension implements ParameterResolver, AfterEachCallback {
    private final LogCaptor logCaptor = new LogCaptor();

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return LogCaptor.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return logCaptor;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        logCaptor.cleanUp();
    }
}
