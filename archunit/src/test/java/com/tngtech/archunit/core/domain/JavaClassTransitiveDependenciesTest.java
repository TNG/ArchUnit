package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

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

    @Test
    public void findsTransitiveDependenciesInAcyclicGraph() {
        Class<?> a = AcyclicGraph.A.class;
        Class<?> b = AcyclicGraph.B.class;
        Class<?> c = AcyclicGraph.C.class;
        Class<?> d = AcyclicGraph.D.class;
        JavaClasses classes = new ClassFileImporter().importClasses(a, b, c, d);
        Class<?> cArray = AcyclicGraph.C[][].class;

        // @formatter:off
        assertThatDependencies(classes.get(a).getTransitiveDependenciesFromSelf())
                .contain(a, Object.class)
                .contain(a, b)
                    .contain(b, Object.class)
                    .contain(b, Integer.class)
                .contain(a, cArray)
                    .contain(c, Object.class)
                    .contain(c, d)
                        .contain(d, Object.class)
                        .contain(d, String.class);

        assertThatDependencies(classes.get(b).getTransitiveDependenciesFromSelf())
                .contain(b, Object.class)
                .contain(b, Integer.class);

        assertThatDependencies(classes.get(c).getTransitiveDependenciesFromSelf())
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
    public void findsTransitiveDependenciesInCyclicGraph() {
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
