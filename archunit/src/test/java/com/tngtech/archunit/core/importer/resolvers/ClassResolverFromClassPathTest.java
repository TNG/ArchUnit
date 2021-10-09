package com.tngtech.archunit.core.importer.resolvers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver.ClassUriImporter;
import com.tngtech.archunit.testutil.TestUtils;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ClassResolverFromClassPathTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClassUriImporter uriImporter;

    private final ClassResolverFromClasspath resolver = new ClassResolverFromClasspath();

    @Before
    public void setUp() {
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

        assertThat(result).isAbsent();
        verifyNoMoreInteractions(uriImporter);
    }

    @DataProvider
    public static Object[][] urls_with_spaces() throws MalformedURLException, URISyntaxException {
        return $$(
                $(new URL("file:/C:/Some Windows/URL with spaces 123/any.jar"), new URI("file:/C:/Some%20Windows/URL%20with%20spaces%20123/any.jar")),
                $(new URL("file:/Some Unix/URL with spaces 123/any.jar"), new URI("file:/Some%20Unix/URL%20with%20spaces%20123/any.jar"))
        );
    }

    @Test
    @UseDataProvider("urls_with_spaces")
    public void is_resilient_against_wrongly_encoded_ClassLoader_resource_URLs(final URL urlReturnedByClassLoader, URI expectedUriDerivedFromUrl) {
        // it seems like some OSGI ClassLoaders incorrectly return URLs with unencoded spaces.
        // This lead to `url.toURI()` throwing an exception -> https://github.com/TNG/ArchUnit/issues/683
        verifyUrlCannotBeConvertedToUriInTheCurrentForm(urlReturnedByClassLoader);

        final JavaClass expectedJavaClass = importClassWithContext(Object.class);
        when(uriImporter.tryImport(expectedUriDerivedFromUrl)).thenReturn(Optional.of(expectedJavaClass));

        Optional<JavaClass> resolvedClass = withMockedContextClassLoader(new Function<ClassLoader, Optional<JavaClass>>() {
            @Override
            public Optional<JavaClass> apply(ClassLoader classLoaderMock) {
                String typeNameFromUrlWithSpaces = "some.TypeFromUrlWithSpaces";
                String typeResourceFromUrlWithSpaces = typeNameFromUrlWithSpaces.replace(".", "/") + ".class";
                when(classLoaderMock.getResource(typeResourceFromUrlWithSpaces)).thenReturn(urlReturnedByClassLoader);

                return resolver.tryResolve(typeNameFromUrlWithSpaces);
            }
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

    private void verifyUrlCannotBeConvertedToUriInTheCurrentForm(final URL url) {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                url.toURI();
            }
        }).isInstanceOf(URISyntaxException.class);
    }
}