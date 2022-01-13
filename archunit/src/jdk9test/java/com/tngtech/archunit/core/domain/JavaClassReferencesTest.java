package com.tngtech.archunit.core.domain;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClassTest.DependencyConditionCreation;
import com.tngtech.archunit.core.domain.testexamples.AReferencingB;
import com.tngtech.archunit.core.domain.testexamples.BReferencedByA;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassReferencesTest {

    @Test
    public void direct_dependencies_from_self_by_references() {
        JavaClass javaClass = importClasses(AReferencingB.class, BReferencedByA.class).get(AReferencingB.class);

        assertReferencesFromAToB(javaClass.getDirectDependenciesFromSelf());
    }

    @Test
    public void direct_dependencies_to_self_by_references() {
        JavaClass javaClass = importClasses(AReferencingB.class, BReferencedByA.class).get(BReferencedByA.class);

        assertReferencesFromAToB(javaClass.getDirectDependenciesToSelf());
    }

    @Test
    public void function_get_code_unit_references_from_self() {
        class Target {
            void target() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            Consumer<Target> origin() {
                Supplier<Target> supplier = Target::new;
                return Target::target;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClasses(Origin.class, Target.class).get(Origin.class);

        assertThat(JavaClass.Functions.GET_CODE_UNIT_REFERENCES_FROM_SELF.apply(javaClass)).isEqualTo(javaClass.getCodeUnitReferencesFromSelf());
        assertThat(JavaClass.Functions.GET_METHOD_REFERENCES_FROM_SELF.apply(javaClass)).isEqualTo(javaClass.getMethodReferencesFromSelf());
        assertThat(JavaClass.Functions.GET_CONSTRUCTOR_REFERENCES_FROM_SELF.apply(javaClass)).isEqualTo(javaClass.getConstructorReferencesFromSelf());
    }

    @Test
    public void function_get_code_unit_references_to_self() {
        class Target {
            void target() {
            }
        }
        @SuppressWarnings("unused")
        class Origin {
            Consumer<Target> origin() {
                Supplier<Target> supplier = Target::new;
                return Target::target;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClasses(Origin.class, Target.class).get(Target.class);

        assertThat(JavaClass.Functions.GET_CODE_UNIT_REFERENCES_TO_SELF.apply(javaClass)).isEqualTo(javaClass.getCodeUnitReferencesToSelf());
        assertThat(JavaClass.Functions.GET_METHOD_REFERENCES_TO_SELF.apply(javaClass)).isEqualTo(javaClass.getMethodReferencesToSelf());
        assertThat(JavaClass.Functions.GET_CONSTRUCTOR_REFERENCES_TO_SELF.apply(javaClass)).isEqualTo(javaClass.getConstructorReferencesToSelf());
    }

    private void assertReferencesFromAToB(Set<Dependency> dependencies) {
        assertThat(dependencies)
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, CONSTRUCTOR_NAME)
                        .inLineNumber(9))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, CONSTRUCTOR_NAME)
                        .inLineNumber(10))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, "getSomeField")
                        .inLineNumber(14))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, "getSomeField")
                        .inLineNumber(15))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, "getNothing")
                        .inLineNumber(16));
    }

    private DependencyConditionCreation referenceDependency() {
        return new DependencyConditionCreation("references");
    }
}
