package com.tngtech.archunit.core.importer;

import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;

public class ClassFileImporterShadowedTypeVariableBoundsTest {

    @Test
    public void resolves_shadowed_type_variables_to_the_innermost_bound() {
        JavaClasses classes = new ClassFileImporter().importClasses(
                C0.class,
                C0.C1.class,
                C0.C1.C2.class,
                C0.C1.C2.C3.class,
                C0.C1.C2.C3.C4.class,
                C0.C1.C2.C3.C4.C5.class,
                C0.C1.C2.C3.C4.C5.C6.class,
                C0.C1.C2.C3.C4.C5.C6.C7.class,
                C0.C1.C2.C3.C4.C5.C6.C7.C8.class,
                C0.C1.C2.C3.C4.C5.C6.C7.C8.C9.class,
                Outer.class,
                Outer.Mid.class,
                Outer.Mid.Inner.class);

        Class<?>[] nested = {
                C0.class,
                C0.C1.class,
                C0.C1.C2.class,
                C0.C1.C2.C3.class,
                C0.C1.C2.C3.C4.class,
                C0.C1.C2.C3.C4.C5.class,
                C0.C1.C2.C3.C4.C5.C6.class,
                C0.C1.C2.C3.C4.C5.C6.C7.class,
                C0.C1.C2.C3.C4.C5.C6.C7.C8.class,
                C0.C1.C2.C3.C4.C5.C6.C7.C8.C9.class
        };
        Class<?>[] bounds = {
                Bound0.class, Bound1.class, Bound2.class, Bound3.class, Bound4.class,
                Bound5.class, Bound6.class, Bound7.class, Bound8.class, Bound9.class
        };

        for (int i = 0; i < nested.length; i++) {
            JavaClass javaClass = classes.get(nested[i]);

            assertThatTypes(javaClass.getSuperclass().get().getAllInvolvedRawTypes())
                    .as("superclass of %s", nested[i].getSimpleName())
                    .matchInAnyOrder(ArrayList.class, bounds[i]);
        }

        JavaClass inner = classes.get(Outer.Mid.Inner.class);
        assertThatTypes(inner.getField("field").getAllInvolvedRawTypes())
                .as("field of Inner")
                .matchInAnyOrder(ArrayList.class, Bound1.class);
        assertThatTypes(inner.getMethod("method", List.class).getAllInvolvedRawTypes())
                .as("method of Inner")
                .matchInAnyOrder(ArrayList.class, List.class, Bound1.class);
    }

    static class Bound0 {
    }

    static class Bound1 {
    }

    static class Bound2 {
    }

    static class Bound3 {
    }

    static class Bound4 {
    }

    static class Bound5 {
    }

    static class Bound6 {
    }

    static class Bound7 {
    }

    static class Bound8 {
    }

    static class Bound9 {
    }

    static class C0<T extends Bound0> extends ArrayList<T> {
        static class C1<T extends Bound1> extends ArrayList<T> {
            static class C2<T extends Bound2> extends ArrayList<T> {
                static class C3<T extends Bound3> extends ArrayList<T> {
                    static class C4<T extends Bound4> extends ArrayList<T> {
                        static class C5<T extends Bound5> extends ArrayList<T> {
                            static class C6<T extends Bound6> extends ArrayList<T> {
                                static class C7<T extends Bound7> extends ArrayList<T> {
                                    static class C8<T extends Bound8> extends ArrayList<T> {
                                        static class C9<T extends Bound9> extends ArrayList<T> {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static class Outer<T extends Bound0> {
        class Mid<T extends Bound1> {
            class Inner {
                @SuppressWarnings("unused")
                ArrayList<T> field;

                @SuppressWarnings("unused")
                <G extends T> ArrayList<T> method(List<G> value) {
                    return null;
                }
            }
        }
    }
}
