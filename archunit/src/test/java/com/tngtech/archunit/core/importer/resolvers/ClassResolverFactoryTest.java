package com.tngtech.archunit.core.importer.resolvers;

import java.util.List;

import com.google.common.base.Preconditions;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.ArchUnitException.ClassResolverConfigurationException;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver.Factory.NoOpClassResolver;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassResolverFactoryTest {
    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule();
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final ClassResolver.Factory resolverFactory = new ClassResolver.Factory();

    @Test
    public void resolver_from_classpath_can_be_switched_by_boolean_flag() {
        ArchConfiguration.get().unsetClassResolver();
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);

        assertThat(resolverFactory.create()).isInstanceOf(NoOpClassResolver.class);

        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(true);

        assertThat(resolverFactory.create()).isInstanceOf(ClassResolverFromClasspath.class);
    }

    @Test
    public void configured_test_resolver_with_args() {
        ArchConfiguration.get().setClassResolver(TestResolver.class);
        ArchConfiguration.get().setClassResolverArguments("firstArg", "secondArg");

        ClassResolver resolver = resolverFactory.create();

        assertThat(resolver).isInstanceOf(TestResolver.class);
        assertThat(((TestResolver) resolver).first).isEqualTo("firstArg");
        assertThat(((TestResolver) resolver).second).isEqualTo("secondArg");
    }

    @Test
    public void configured_test_resolver_without_args() {
        ArchConfiguration.get().setClassResolver(ResolverWithDefaultConstructor.class);
        ArchConfiguration.get().setClassResolverArguments();

        ClassResolver resolver = resolverFactory.create();

        assertThat(resolver).isInstanceOf(ResolverWithDefaultConstructor.class);
    }

    @Test
    public void wrong_resolver_class_name() {
        ArchConfiguration.get().setProperty("classResolver", "not.There");

        thrown.expect(ClassResolverConfigurationException.class);
        thrown.expectMessage("Error loading resolver class not.There");

        resolverFactory.create();
    }

    @Test
    public void wrong_resolver_constructor() {
        ArchConfiguration.get().setClassResolver(ResolverWithWrongConstructor.class);
        ArchConfiguration.get().setClassResolverArguments("irrelevant");

        expectWrongConstructorException(ResolverWithWrongConstructor.class, "'irrelevant'");

        resolverFactory.create();
    }

    @Test
    public void wrong_resolver_args() {
        ArchConfiguration.get().setClassResolver(ResolverWithDefaultConstructor.class);
        ArchConfiguration.get().setClassResolverArguments("too", "many");

        expectWrongConstructorException(ResolverWithDefaultConstructor.class, "'too', 'many'");

        resolverFactory.create();
    }

    @Test
    public void exception_while_creating_resolver() {
        ArchConfiguration.get().setClassResolver(ExceptionThrowingTestResolver.class);
        ArchConfiguration.get().setClassResolverArguments("bummer");

        thrown.expect(ClassResolverConfigurationException.class);
        thrown.expectMessage("class " + ExceptionThrowingTestResolver.class.getName() +
                " threw an exception in constructor " + ExceptionThrowingTestResolver.class.getSimpleName() + "('bummer')");
        thrown.expect(causeWithMessageContaining("bummer"));

        resolverFactory.create();
    }

    private void expectWrongConstructorException(Class<?> resolverClass, String params) {
        thrown.expect(ClassResolverConfigurationException.class);
        thrown.expectMessage("class " + resolverClass.getName() +
                " has no constructor taking a single argument of type java.util.List, to accept configured parameters " +
                "[" + params + "]");
    }

    private Matcher<Throwable> causeWithMessageContaining(final String message) {
        return new TypeSafeMatcher<Throwable>() {
            @Override
            protected boolean matchesSafely(Throwable item) {
                return item.getCause() != null &&
                        item.getCause().getCause() != null &&
                        item.getCause().getCause().getMessage().contains(message);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("cause with cause with message containing '%s'", message));
            }
        };
    }

    static class TestResolver implements ClassResolver {
        final String first;
        final String second;

        public TestResolver(List<String> args) {
            Preconditions.checkArgument(args.size() == 2);
            this.first = args.get(0);
            this.second = args.get(1);
        }

        @Override
        public void setClassUriImporter(ClassUriImporter classUriImporter) {
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName) {
            return Optional.absent();
        }
    }

    static class ExceptionThrowingTestResolver implements ClassResolver {
        public ExceptionThrowingTestResolver(List<String> args) {
            throw new RuntimeException(args.get(0));
        }

        @Override
        public void setClassUriImporter(ClassUriImporter classUriImporter) {
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName) {
            return Optional.absent();
        }
    }

    static class ResolverWithDefaultConstructor implements ClassResolver {
        public ResolverWithDefaultConstructor() {
        }

        @Override
        public void setClassUriImporter(ClassUriImporter classUriImporter) {
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName) {
            return Optional.absent();
        }
    }

    static class ResolverWithWrongConstructor implements ClassResolver {
        ResolverWithWrongConstructor(String bummer) {
        }

        @Override
        public void setClassUriImporter(ClassUriImporter classUriImporter) {
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName) {
            return Optional.absent();
        }
    }
}