package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolverFromClasspath;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ClassFileProcessorTest {
    @Test
    public void ClassResolverFromClassPath_resolves_robustly() {
        Optional<JavaClass> resolved = new ClassResolverFromClasspath()
                .tryResolve("not.There");

        assertThat(resolved).isAbsent();
    }
}