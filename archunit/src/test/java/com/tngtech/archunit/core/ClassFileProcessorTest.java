package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.Set;

import com.tngtech.archunit.core.ClassFileProcessor.ClassResolverFromClassPath;
import com.tngtech.archunit.core.JavaClassProcessor.DeclarationHandler;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ClassFileProcessorTest {
    @Test
    public void ClassResolverFromClassPath_resolves_robustly() {
        JavaClass resolved = new ClassResolverFromClassPath(mock(DeclarationHandler.class)).resolve("not.There");

        assertThat(resolved.getName()).isEqualTo("not.There");
        assertThat(resolved.getMethods()).isEmpty();
        assertThat(resolved.getConstructors()).isEmpty();
        assertThat(resolved.getFields()).isEmpty();
        assertThat(resolved.getStaticInitializer()).isAbsent();
        assertThat(resolved.getAccessesFromSelf()).isEmpty();
        assertThat(resolved.getSuperClass()).isAbsent();
        assertThat(resolved.getSubClasses()).isEmpty();
        assertThat(resolved.isInterface()).isFalse(); // NOTE: We can't determine this, so by default a non determinable type is no interface
    }

    @Test
    public void ClassResolverFromClassPath_gets_superclasses_robustly() {
        Set<JavaClass> resolved = new ClassResolverFromClassPath(mock(DeclarationHandler.class))
                .getAllSuperClasses("not.There", Collections.<String, JavaClass>emptyMap());

        assertThat(resolved).as("Superclasses").isEmpty();
    }
}