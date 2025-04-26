package com.tngtech.archunit.core.domain;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_TRANSITIVE_DEPENDENCIES_FROM_SELF;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;

public class JavaClassTransitiveDependenciesTest {

    @SuppressWarnings("unused")
    static class AcyclicGraph {
        static class A {
            B b;
            C[][] c;
        }

        static class B {
            Integer i;
        }

        static class C {
            D d;
        }

        static class D {
            String s;
        }
    }

    static Stream<Function<JavaClass, Set<Dependency>>> finds_transitive_dependencies_in_acyclic_graph() {
        return Stream.of(
                JavaClass::getTransitiveDependenciesFromSelf,
                GET_TRANSITIVE_DEPENDENCIES_FROM_SELF
        );
    }

    @ParameterizedTest
    @MethodSource
    void finds_transitive_dependencies_in_acyclic_graph(Function<JavaClass, Set<Dependency>> getTransitiveDependenciesFromSelf) {
        Class<?> a = AcyclicGraph.A.class;
        Class<?> b = AcyclicGraph.B.class;
        Class<?> c = AcyclicGraph.C.class;
        Class<?> d = AcyclicGraph.D.class;
        JavaClasses classes = new ClassFileImporter().importClasses(a, b, c, d);
        Class<?> cArray = AcyclicGraph.C[][].class;

        // @formatter:off
        assertThatDependencies(getTransitiveDependenciesFromSelf.apply(classes.get(a)))
                .contain(a, Object.class)
                .contain(a, b)
                    .contain(b, Object.class)
                    .contain(b, Integer.class)
                .contain(a, cArray)
                    .contain(c, Object.class)
                    .contain(c, d)
                        .contain(d, Object.class)
                        .contain(d, String.class);

        assertThatDependencies(getTransitiveDependenciesFromSelf.apply(classes.get(b)))
                .contain(b, Object.class)
                .contain(b, Integer.class);

        assertThatDependencies(getTransitiveDependenciesFromSelf.apply(classes.get(c)))
                .contain(c, Object.class)
                .contain(c, d)
                    .contain(d, Object.class)
                    .contain(d, String.class);
        // @formatter:on
    }

    @SuppressWarnings("unused")
    static class CyclicGraph {
        static class A {
            B b;
            C[][] c;
            D d;
        }

        static class B {
            Integer i;
        }

        static class C {
            A a;
        }

        static class D {
            E e;
        }

        static class E {
            A a;
            String s;
        }
    }

    @Test
    public void finds_transitive_dependencies_in_cyclic_graph() {
        Class<?> a = CyclicGraph.A.class;
        Class<?> b = CyclicGraph.B.class;
        Class<?> c = CyclicGraph.C.class;
        Class<?> d = CyclicGraph.D.class;
        Class<?> e = CyclicGraph.E.class;
        JavaClasses classes = new ClassFileImporter().importClasses(a, b, c, d, e);
        Class<?> cArray = CyclicGraph.C[][].class;

        // @formatter:off
        assertThatDependencies(classes.get(a).getTransitiveDependenciesFromSelf())
                .contain(a, Object.class)
                .contain(a, b)
                    .contain(b, Object.class)
                    .contain(b, Integer.class)
                .contain(a, cArray)
                    .contain(c, Object.class)
                    .contain(c, a)
                .contain(a, d)
                    .contain(d, Object.class)
                    .contain(d, e)
                        .contain(e, Object.class)
                        .contain(e, a)
                        .contain(e, String.class);

        assertThatDependencies(classes.get(c).getTransitiveDependenciesFromSelf())
                .contain(c, Object.class)
                .contain(c, a)
                    .contain(a, Object.class)
                    .contain(a, b)
                        .contain(b, Object.class)
                        .contain(b, Integer.class)
                    .contain(a, cArray)
                    .contain(a, d)
                        .contain(d, Object.class)
                        .contain(d, e)
                            .contain(e, Object.class)
                            .contain(e, a)
                            .contain(e, String.class);

        assertThatDependencies(classes.get(d).getTransitiveDependenciesFromSelf())
                .contain(d, Object.class)
                .contain(d, e)
                    .contain(e, Object.class)
                    .contain(e, a)
                        .contain(a, Object.class)
                        .contain(a, b)
                            .contain(b, Object.class)
                            .contain(b, Integer.class)
                        .contain(a, cArray)
                            .contain(c, Object.class)
                            .contain(c, a)
                        .contain(a, d)
                    .contain(e, String.class);
        // @formatter:on
    }
}
