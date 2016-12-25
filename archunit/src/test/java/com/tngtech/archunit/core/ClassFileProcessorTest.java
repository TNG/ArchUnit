package com.tngtech.archunit.core;

import java.util.Map;

import com.tngtech.archunit.core.ClassFileProcessor.ClassResolverFromClassPath;
import com.tngtech.archunit.core.JavaClassProcessor.DeclarationHandler;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ClassFileProcessorTest {
    @Test
    public void ClassResolverFromClassPath_resolves_robustly() {
        Optional<JavaClass> resolved = new ClassResolverFromClassPath(mock(DeclarationHandler.class))
                .resolve("not.There", mock(ImportedClasses.ByTypeName.class));

        assertThat(resolved).isAbsent();
    }

    @Test
    public void ClassResolverFromClassPath_gets_superclasses_robustly() {
        Map<String, Optional<JavaClass>> resolved = new ClassResolverFromClassPath(mock(DeclarationHandler.class))
                .getAllSuperClasses("not.There", mock(ImportedClasses.ByTypeName.class));

        assertThat(resolved).as("Superclasses").isEmpty();
    }
}