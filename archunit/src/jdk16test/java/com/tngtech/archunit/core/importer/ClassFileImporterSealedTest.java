package com.tngtech.archunit.core.importer;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.TestUtils.relativeResourceUri;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

class ClassFileImporterSealedTest {

    @Test
    void regular_class_is_not_sealed() {
        JavaClass regular = new ClassFileImporter().importClass(ClassFileImporterSealedTest.class);

        assertNonSealed(regular);
    }

    @Nested
    class TestSealedClass {
        @Test
        void sealed_class_with_permitted_subclasses_is_sealed() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedClass.class,
                    SealedClass.Impl.class, SealedClass.NonSealedSubclass.class, SealedClass.SealedSubclass.class);

            assertSealedWithPermittedSubclasses(classes.get(SealedClass.class),
                    SealedClass.Impl.class, SealedClass.NonSealedSubclass.class, SealedClass.SealedSubclass.class);
        }

        @Test
        void permitted_subclasses_are_resolved_from_classpath() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedClass.class);

            assertSealedWithPermittedSubclasses(classes.get(SealedClass.class),
                    SealedClass.Impl.class, SealedClass.NonSealedSubclass.class, SealedClass.SealedSubclass.class);
        }

        @Test
        void permitted_subclass_is_not_sealed() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedClass.class,
                    SealedClass.Impl.class, SealedClass.NonSealedSubclass.class, SealedClass.SealedSubclass.class);

            assertNonSealed(classes.get(SealedClass.Impl.class));
        }

        @Test
        void non_sealed_subclass_is_not_sealed() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedClass.class,
                    SealedClass.Impl.class, SealedClass.NonSealedSubclass.class, SealedClass.SealedSubclass.class);

            assertNonSealed(classes.get(SealedClass.NonSealedSubclass.class));
        }

        @Test
        void sealed_permitted_subclass_is_sealed_with_its_own_subclasses() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedClass.class,
                    SealedClass.Impl.class, SealedClass.NonSealedSubclass.class, SealedClass.SealedSubclass.class,
                    SealedClass.SealedSubclass.ImplA.class, SealedClass.SealedSubclass.ImplB.class);

            assertSealedWithPermittedSubclasses(classes.get(SealedClass.SealedSubclass.class),
                    SealedClass.SealedSubclass.ImplA.class, SealedClass.SealedSubclass.ImplB.class);
        }
    }

    @Test
    void regular_interface_is_not_sealed() {
        JavaClass regular = new ClassFileImporter().importClass(AutoCloseable.class);

        assertNonSealed(regular);
    }

    @Nested
    class TestSealedInterface {
        @Test
        void sealed_interface_with_permitted_subclasses_is_sealed() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedInterface.class,
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class);

            assertSealedWithPermittedSubclasses(classes.get(SealedInterface.class),
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class);
        }

        @Test
        void permitted_subclasses_are_resolved_from_classpath() {
            JavaClass sealed = new ClassFileImporter().importClass(SealedInterface.class);

            assertSealedWithPermittedSubclasses(sealed,
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class);
        }

        @Test
        void permitted_implementation_is_not_sealed() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedInterface.class,
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class);

            assertNonSealed(classes.get(SealedInterface.Impl.class));
        }

        @Test
        void non_sealed_implementation_is_not_sealed() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedInterface.class,
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class);

            assertNonSealed(classes.get(SealedInterface.NonSealedSubclass.class));
        }

        @Test
        void non_sealed_subinterface_is_not_sealed() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedInterface.class,
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class);

            assertNonSealed(classes.get(SealedInterface.NonSealedSubinterface.class));
        }

        @Test
        void sealed_permitted_subclass_is_sealed_with_its_own_subclasses() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedInterface.class,
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class,
                    SealedInterface.SealedSubclass.ImplA.class, SealedInterface.SealedSubclass.ImplB.class);

            assertSealedWithPermittedSubclasses(classes.get(SealedInterface.SealedSubclass.class),
                    SealedInterface.SealedSubclass.ImplA.class, SealedInterface.SealedSubclass.ImplB.class);
        }

        @Test
        void sealed_permitted_subinterface_is_sealed_with_its_own_subclasses() {
            JavaClasses classes = new ClassFileImporter().importClasses(SealedInterface.class,
                    SealedInterface.Impl.class, SealedInterface.NonSealedSubclass.class, SealedInterface.NonSealedSubinterface.class,
                    SealedInterface.SealedSubclass.class, SealedInterface.SealedSubinterface.class,
                    SealedInterface.SealedSubinterface.ImplA.class, SealedInterface.SealedSubinterface.ImplB.class);

            assertSealedWithPermittedSubclasses(classes.get(SealedInterface.SealedSubinterface.class),
                    SealedInterface.SealedSubinterface.ImplA.class, SealedInterface.SealedSubinterface.ImplB.class
            );
        }
    }

    /**
     * The Java compiler would not create such bytecode (Java requires at least one {@code TypeName} in the
     * <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.1.6">{@code permits} clause</a> of a class declaration),
     * but it is <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.31">technically valid</a>:
     * <p>
     * {@code EmptyPermittedSubclasses.class.getPermittedSubclasses() == new Class[0] != null},
     * i.e. {@code EmptyPermittedSubclasses.class.isSealed() == true}.
     * <p>
     * As ASM's {@link ClassReader} invokes {@link ClassVisitor#visitPermittedSubclass(String)}
     * for every permitted subclass, ArchUnit cannot know of the (empty) permitted subclasses from this information.
     */
    @Test
    void empty_permitted_subclasses_are_currently_not_recognized_by_ArchUnit() {
        // This .class file was created by dropping the permitted subclass from the byte code of a simple sealed class initially compiled with Java.
        var location = Location.of(relativeResourceUri(getClass(), "EmptyPermittedSubclasses.class"));
        JavaClass emptyPermittedSubclasses = getOnlyElement(new ClassFileImporter().importLocations(List.of(location)));

        assertThat(emptyPermittedSubclasses.isSealed()).isFalse(); // current behavior; actually expected: .isTrue();
        assertThat(emptyPermittedSubclasses.getPermittedSubclasses()).isEmpty(); // current behavior; actually expected: .isNotEmpty();
        assertSealedWithPermittedSubclasses(emptyPermittedSubclasses.reflect(), new Class[0]);
    }

    void assertSealedWithPermittedSubclasses(JavaClass javaClass, Class<?>... permittedSubclasses) {
        assertThat(javaClass.isSealed())
                .as("%s.isSealed()", javaClass)
                .isTrue();
        assertThat(javaClass.getPermittedSubclasses())
                .as("%s.getPermittedSubclasses()", javaClass)
                .isPresent();
        assertThat(javaClass.getPermittedSubclasses().get())
                .extractingResultOf("getName")
                .as("names of %s.getPermittedSubclasses()", javaClass)
                .containsExactly(stream(permittedSubclasses).map(Class::getName).toArray());

        assertSealedWithPermittedSubclasses(javaClass.reflect(), permittedSubclasses);
    }

    private static void assertSealedWithPermittedSubclasses(Class<?> reflected, Class<?>... permittedSubclasses) {
        assertThat(reflected.isSealed())
                .as("reflected %s.isSealed()", reflected)
                .isTrue();
        assertThat(reflected.getPermittedSubclasses())
                .as("reflected %s.getPermittedSubclasses()", reflected)
                .containsExactly(permittedSubclasses);
    }

    void assertNonSealed(JavaClass javaClass) {
        assertThat(javaClass.isSealed())
                .as("%s.isSealed()", javaClass)
                .isFalse();
        assertThat(javaClass.getPermittedSubclasses())
                .as("%s.getPermittedSubclasses()", javaClass)
                .isEmpty();

        Class<?> reflected = javaClass.reflect();
        assertThat(reflected.isSealed())
                .as("reflected %s.isSealed()", reflected)
                .isFalse();
        assertThat(reflected.getPermittedSubclasses())
                .as("reflected %s.getPermittedSubclasses()", reflected)
                .isNull();
    }

    sealed class SealedClass
            permits SealedClass.Impl, SealedClass.NonSealedSubclass, SealedClass.SealedSubclass {

        final class Impl extends SealedClass {
        }

        non-sealed class NonSealedSubclass extends SealedClass {

            class ImplA extends NonSealedSubclass {
            }

            class ImplB extends NonSealedSubclass {
            }
        }

        sealed class SealedSubclass extends SealedClass
                permits SealedSubclass.ImplA, SealedSubclass.ImplB {

            final class ImplA extends SealedSubclass {
            }

            final class ImplB extends SealedSubclass {
            }
        }
    }

    sealed interface SealedInterface
            permits SealedInterface.Impl, SealedInterface.NonSealedSubclass, SealedInterface.NonSealedSubinterface,
            SealedInterface.SealedSubclass, SealedInterface.SealedSubinterface {

        final class Impl implements SealedInterface {
        }

        non-sealed class NonSealedSubclass implements SealedInterface {

            class ImplA extends NonSealedSubclass {
            }

            class ImplB extends NonSealedSubclass {
            }
        }

        non-sealed interface NonSealedSubinterface extends SealedInterface {

            class ImplA implements NonSealedSubinterface {
            }

            class ImplB implements NonSealedSubinterface {
            }
        }

        sealed class SealedSubclass implements SealedInterface
                permits SealedSubclass.ImplA, SealedSubclass.ImplB {

            final class ImplA extends SealedSubclass {

            }
            final class ImplB extends SealedSubclass {
            }
        }

        /** has implicitly permitted implementations */
        sealed interface SealedSubinterface extends SealedInterface {

            final class ImplA implements SealedSubinterface {

            }
            final class ImplB implements SealedSubinterface {
            }
        }
    }
}
