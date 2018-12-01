package com.tngtech.archunit.core.importer.resolvers;

import java.net.URI;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver.ClassUriImporter;
import com.tngtech.archunit.core.importer.resolvers.testclasses.firstdependency.FirstDependency;
import com.tngtech.archunit.core.importer.resolvers.testclasses.seconddependency.sub.SecondDependency;
import com.tngtech.archunit.core.importer.resolvers.testclasses.thirddependency.ThirdDependency;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SelectedClassResolverFromClasspathTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClassUriImporter classUriImporter;

    @Test
    public void resolves_exactly_the_classes_beneath_the_given_packages_from_the_classpath() {
        simulatePossibleUriImportOf(FirstDependency.class, SecondDependency.class);

        SelectedClassResolverFromClasspath resolver =
                new SelectedClassResolverFromClasspath(packages("firstdependency", "seconddependency"));
        resolver.setClassUriImporter(classUriImporter);

        assertResolved(resolver.tryResolve(FirstDependency.class.getName()), FirstDependency.class);
        assertResolved(resolver.tryResolve(SecondDependency.class.getName()), SecondDependency.class);
        assertThat(resolver.tryResolve(ThirdDependency.class.getName())).isAbsent();
        verifyNoMoreInteractions(classUriImporter);
    }

    private void simulatePossibleUriImportOf(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            JavaClass classToReturn = importClassWithContext(clazz);
            doReturn(Optional.of(classToReturn)).when(classUriImporter).tryImport(uriFor(clazz));
        }
    }

    private void assertResolved(Optional<JavaClass> resolved, Class<?> expected) {
        verify(classUriImporter).tryImport(uriFor(expected));
        assertThat(resolved).isPresent();
        assertThat(resolved.get().isEquivalentTo(expected))
                .as("%s is equivalent to %s", resolved.get(), expected.getSimpleName())
                .isTrue();
    }

    private URI uriFor(final Class<?> clazz) {
        return argThat(new ArgumentMatcher<URI>() {
            @Override
            public boolean matches(URI argument) {
                return argument.toString().contains(clazz.getSimpleName());
            }
        });
    }

    private ImmutableList<String> packages(String... subPackages) {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (String pkg : subPackages) {
            result.add(getClass().getPackage().getName() + ".testclasses." + pkg);
        }
        return result.build();
    }
}