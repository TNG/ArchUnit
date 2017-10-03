package com.tngtech.archunit.core.importer.resolvers;

import java.net.URI;
import java.net.URISyntaxException;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver.ClassUriImporter;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ClassResolverFromClassPathTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClassUriImporter uriImporter;

    private ClassResolverFromClasspath resolver = new ClassResolverFromClasspath();

    @Test
    public void finds_uri_of_class_on_classpath() throws URISyntaxException {
        JavaClass expectedJavaClass = importClassWithContext(Object.class);
        when(uriImporter.tryImport(uriOf(Object.class))).thenReturn(Optional.of(expectedJavaClass));

        resolver.setClassUriImporter(uriImporter);

        Optional<JavaClass> result = resolver.tryResolve(Object.class.getName());

        assertThat(result).contains(expectedJavaClass);
    }

    @Test
    public void is_resilient_if_URI_cant_be_located() throws URISyntaxException {
        resolver.setClassUriImporter(uriImporter);

        Optional<JavaClass> result = resolver.tryResolve("sooo.Wrong");

        assertThat(result).isAbsent();
        verifyNoMoreInteractions(uriImporter);
    }

    private URI uriOf(Class<?> clazz) throws URISyntaxException {
        return getClass().getResource("/" + clazz.getName().replace('.', '/') + ".class").toURI();
    }
}