package com.tngtech.archunit.core.importer.resolvers;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.ArchUnitException.ClassResolverConfigurationException;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver.Factory.NoOpClassResolver;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.archunit.testutil.ContextClassLoaderRule;
import com.tngtech.archunit.testutil.OutsideOfClassPathRule;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClassResolverFactoryTest {
    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule();
    @Rule
    public final OutsideOfClassPathRule outsideOfClassPathRule = new OutsideOfClassPathRule();
    @Rule
    public final ContextClassLoaderRule contextClassLoaderRule = new ContextClassLoaderRule();

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

        assertThatThrownBy(resolverFactory::create)
                .isInstanceOf(ClassResolverConfigurationException.class)
                .hasMessage("Error loading resolver class not.There");
    }

    @Test
    public void wrong_resolver_constructor() {
        ArchConfiguration.get().setClassResolver(ResolverWithWrongConstructor.class);
        ArchConfiguration.get().setClassResolverArguments("irrelevant");

        expectWrongConstructorException(resolverFactory::create, ResolverWithWrongConstructor.class, "'irrelevant'");
    }

    @Test
    public void wrong_resolver_args() {
        ArchConfiguration.get().setClassResolver(ResolverWithDefaultConstructor.class);
        ArchConfiguration.get().setClassResolverArguments("too", "many");

        expectWrongConstructorException(resolverFactory::create, ResolverWithDefaultConstructor.class, "'too', 'many'");
    }

    @Test
    public void exception_while_creating_resolver() {
        ArchConfiguration.get().setClassResolver(ExceptionThrowingTestResolver.class);
        ArchConfiguration.get().setClassResolverArguments("bummer");

        assertThatThrownBy(resolverFactory::create)
                .isInstanceOf(ClassResolverConfigurationException.class)
                .hasMessage("class %s threw an exception in constructor %s('bummer')",
                        ExceptionThrowingTestResolver.class.getName(), ExceptionThrowingTestResolver.class.getSimpleName())
                .rootCause().hasMessageContaining("bummer");
    }

    @Test
    public void loads_resolver_from_context_ClassLoader() throws IOException {
        Path targetDir = outsideOfClassPathRule
                .compileClassesFrom(getClass().getResource("testclasses/someresolver"))
                .getPath();

        String resolverClassName = "com.tngtech.archunit.core.importer.resolvers.testclasses.someresolver.SomeResolver";
        ArchConfiguration.get().setProperty("classResolver", resolverClassName);

        URLClassLoader classLoaderThatKnowsResolver = new URLClassLoader(new URL[]{targetDir.toUri().toURL()}, getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoaderThatKnowsResolver);

        ClassResolver resolver = resolverFactory.create();

        assertThat(resolver.getClass().getName()).isEqualTo(resolverClassName);
    }

    private void expectWrongConstructorException(ThrowingCallable callable, Class<?> resolverClass, String params) {
        assertThatThrownBy(callable)
                .isInstanceOf(ClassResolverConfigurationException.class)
                .hasMessage("class %s has no constructor taking a single argument of type java.util.List, to accept configured parameters [%s]",
                        resolverClass.getName(), params);
    }

    private static class TestResolver implements ClassResolver {
        final String first;
        final String second;

        private TestResolver(List<String> args) {
            Preconditions.checkArgument(args.size() == 2);
            this.first = args.get(0);
            this.second = args.get(1);
        }

        @Override
        public void setClassUriImporter(ClassUriImporter classUriImporter) {
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName) {
            return Optional.empty();
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
            return Optional.empty();
        }
    }

    private static class ResolverWithDefaultConstructor implements ClassResolver {
        private ResolverWithDefaultConstructor() {
        }

        @Override
        public void setClassUriImporter(ClassUriImporter classUriImporter) {
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName) {
            return Optional.empty();
        }
    }

    static class ResolverWithWrongConstructor implements ClassResolver {
        @SuppressWarnings("unused")
        ResolverWithWrongConstructor(String bummer) {
        }

        @Override
        public void setClassUriImporter(ClassUriImporter classUriImporter) {
        }

        @Override
        public Optional<JavaClass> tryResolve(String typeName) {
            return Optional.empty();
        }
    }
}
