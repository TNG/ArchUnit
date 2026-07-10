package com.tngtech.archunit.core.importer;

import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassFileImporterCodeUnitReferencesNewerJavaVersionTest {
    /**
     * A local class constructor obtains extra parameters from the outer scope that the compiler transparently adds
     * to the byte code. A reference to this local constructor will then always be translated to a lambda call.
     * Thus, in this case we do not expect a constructor reference.
     *
     * Note that this actually does not compile with JDK 8
     */
    @Test
    public void does_not_import_local_constructor_references() {
        @SuppressWarnings("unused")
        class ReferencedTarget {
            ReferencedTarget() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            void referencesConstructor() {
                Supplier<ReferencedTarget> a = ReferencedTarget::new;
            }
        }

        JavaClasses javaClasses = new ClassFileImporter().importClasses(Origin.class, ReferencedTarget.class);

        assertThat(javaClasses.get(Origin.class).getMethod("referencesConstructor").getConstructorReferencesFromSelf()).isEmpty();
        assertThat(javaClasses.get(ReferencedTarget.class).getConstructor(ClassFileImporterCodeUnitReferencesNewerJavaVersionTest.class).getReferencesToSelf()).isEmpty();
    }
}
