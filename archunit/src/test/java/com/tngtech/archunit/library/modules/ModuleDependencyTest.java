package com.tngtech.archunit.library.modules;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one.ClassOne;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.ClassTwo;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThatConversionOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDependencyTest {
    @Test
    public void can_be_converted_to_dependencies() {
        ArchModules<?> modules = ArchModules.defineByPackages("..test_modules.(*)..")
                .modularize(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class));

        ModuleDependency<?> moduleDependency = createDependency(modules, "one", "two");

        assertThatConversionOf(moduleDependency)
                .satisfiesStandardConventions()
                .isPossibleTo(Dependency.class);
    }

    @Test
    public void description_orders_class_dependency_descriptions_lexicographically_independent_of_encounter_order() {
        JavaClasses classes = new ClassFileImporter().importClasses(AOrigin.class, ZOrigin.class, Target.class);
        JavaClass aOrigin = classes.get(AOrigin.class);
        JavaClass zOrigin = classes.get(ZOrigin.class);
        JavaClass target = classes.get(Target.class);

        ModuleDependency<?> dependencyZThenA = createDependency(linkedSet(zOrigin, aOrigin), target);
        ModuleDependency<?> dependencyAThenZ = createDependency(linkedSet(aOrigin, zOrigin), target);

        Dependency aDependency = dependencyFrom(aOrigin, dependencyAThenZ);
        Dependency zDependency = dependencyFrom(zOrigin, dependencyAThenZ);
        assertThat(zDependency.compareTo(aDependency)).isLessThan(0);
        assertThat(aDependency.getDescription()).isLessThan(zDependency.getDescription());
        assertThat(originClassNames(dependencyZThenA)).containsExactly(ZOrigin.class.getName(), AOrigin.class.getName());
        assertThat(originClassNames(dependencyAThenZ)).containsExactly(AOrigin.class.getName(), ZOrigin.class.getName());

        String expected = String.format("Module Dependency [origin -> target]:%n%s%n%s",
                aDependency.getDescription(), zDependency.getDescription());
        assertThat(dependencyZThenA.getDescription()).isEqualTo(expected);
        assertThat(dependencyAThenZ.getDescription()).isEqualTo(expected);
    }

    private static <D extends ArchModule.Descriptor> ModuleDependency<D> createDependency(ArchModules<D> modules, String originIdentifier, String targetIdentifier) {
        return ModuleDependency.tryCreate(
                modules.getByIdentifier(originIdentifier),
                modules.getByIdentifier(targetIdentifier)
        ).get();
    }

    private static ModuleDependency<?> createDependency(Set<JavaClass> originClasses, JavaClass targetClass) {
        ArchModule<ArchModule.Descriptor> origin = new ArchModule<>(
                ArchModule.Identifier.from("origin"), ArchModule.Descriptor.create("origin"), originClasses);
        ArchModule<ArchModule.Descriptor> target = new ArchModule<>(
                ArchModule.Identifier.from("target"), ArchModule.Descriptor.create("target"), singleton(targetClass));
        return ModuleDependency.tryCreate(origin, target).get();
    }

    private static Set<JavaClass> linkedSet(JavaClass first, JavaClass second) {
        return new LinkedHashSet<>(asList(first, second));
    }

    private static Dependency dependencyFrom(JavaClass origin, ModuleDependency<?> moduleDependency) {
        return moduleDependency.toClassDependencies().stream()
                .filter(dependency -> dependency.getOriginClass().equals(origin))
                .findFirst().get();
    }

    private static String[] originClassNames(ModuleDependency<?> moduleDependency) {
        return moduleDependency.toClassDependencies().stream()
                .map(dependency -> dependency.getOriginClass().getName())
                .toArray(String[]::new);
    }

    static class ZOrigin {
        void callTarget() {
            Target.call();
        }
    }

    static class AOrigin {
        void callTarget() {
            Target.call();
        }
    }

    static class Target {
        static void call() {
        }
    }
}
