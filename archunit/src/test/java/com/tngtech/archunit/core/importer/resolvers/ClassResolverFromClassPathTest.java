package com.tngtech.archunit.core.importer.resolvers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver.ClassUriImporter;
import com.tngtech.archunit.testutil.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassResolverFromClassPathTest {

    @Mock
    private ClassUriImporter uriImporter;

    private final ClassResolverFromClasspath resolver = new ClassResolverFromClasspath();

    @BeforeEach
    void setUp() {
        resolver.setClassUriImporter(uriImporter);
    }

    @Test
    public void finds_uri_of_class_on_classpath() {
        JavaClass expectedJavaClass = importClassWithContext(Object.class);
        when(uriImporter.tryImport(TestUtils.uriOf(Object.class))).thenReturn(Optional.of(expectedJavaClass));

        Optional<JavaClass> result = resolver.tryResolve(Object.class.getName());

        assertThat(result).contains(expectedJavaClass);
    }

    @Test
    public void is_resilient_if_URI_cant_be_located() {
        Optional<JavaClass> result = resolver.tryResolve("sooo.Wrong");

        assertThat(result).isEmpty();
        verifyNoMoreInteractions(uriImporter);
    }

    static Stream<Arguments> urls_with_spaces() throws MalformedURLException, URISyntaxException {
        return Stream.of(
                $(new URL("file:/C:/Some Windows/URL with spaces 123/any.jar"), new URI("file:/C:/Some%20Windows/URL%20with%20spaces%20123/any.jar")),
                $(new URL("file:/Some Unix/URL with spaces 123/any.jar"), new URI("file:/Some%20Unix/URL%20with%20spaces%20123/any.jar"))
        );
    }

    @ParameterizedTest
    @MethodSource("urls_with_spaces")
    void is_resilient_against_wrongly_encoded_ClassLoader_resource_URLs(URL urlReturnedByClassLoader, URI expectedUriDerivedFromUrl) {
        // it seems like some OSGI ClassLoaders incorrectly return URLs with unencoded spaces.
        // This lead to `url.toURI()` throwing an exception -> https://github.com/TNG/ArchUnit/issues/683
        verifyUrlCannotBeConvertedToUriInTheCurrentForm(urlReturnedByClassLoader);

        JavaClass expectedJavaClass = importClassWithContext(Object.class);
        when(uriImporter.tryImport(expectedUriDerivedFromUrl)).thenReturn(Optional.of(expectedJavaClass));

        Optional<JavaClass> resolvedClass = withMockedContextClassLoader(classLoaderMock -> {
            String typeNameFromUrlWithSpaces = "some.TypeFromUrlWithSpaces";
            String typeResourceFromUrlWithSpaces = typeNameFromUrlWithSpaces.replace(".", "/") + ".class";
            when(classLoaderMock.getResource(typeResourceFromUrlWithSpaces)).thenReturn(urlReturnedByClassLoader);

            return resolver.tryResolve(typeNameFromUrlWithSpaces);
        });

        assertThat(resolvedClass).contains(expectedJavaClass);
    }

    private <T> T withMockedContextClassLoader(Function<ClassLoader, T> doWithClassLoader) {
        ClassLoader classLoaderMock = mock(ClassLoader.class);
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoaderMock);
            return doWithClassLoader.apply(classLoaderMock);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private void verifyUrlCannotBeConvertedToUriInTheCurrentForm(URL url) {
        assertThatThrownBy(url::toURI).isInstanceOf(URISyntaxException.class);
    }
}
