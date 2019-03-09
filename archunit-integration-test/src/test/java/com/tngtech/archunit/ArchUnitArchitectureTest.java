package com.tngtech.archunit;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.DomainBuilders;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.origin;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.has;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(
        packagesOf = ArchUnitArchitectureTest.class,
        importOptions = ArchUnitArchitectureTest.ArchUnitProductionCode.class)
public class ArchUnitArchitectureTest {
    static final String THIRDPARTY_PACKAGE_IDENTIFIER = "..thirdparty..";

    @ArchTest
    public static final ArchRule layers_are_respected = layeredArchitecture()
            .layer("Root").definedBy("com.tngtech.archunit")
            .layer("Base").definedBy("com.tngtech.archunit.base..")
            .layer("Core").definedBy("com.tngtech.archunit.core..")
            .layer("Lang").definedBy("com.tngtech.archunit.lang..")
            .layer("Library").definedBy("com.tngtech.archunit.library..")
            .layer("JUnit").definedBy("com.tngtech.archunit.junit..")

            .whereLayer("JUnit").mayNotBeAccessedByAnyLayer()
            .whereLayer("Library").mayOnlyBeAccessedByLayers("JUnit")
            .whereLayer("Lang").mayOnlyBeAccessedByLayers("Library", "JUnit")
            .whereLayer("Core").mayOnlyBeAccessedByLayers("Lang", "Library", "JUnit")
            .whereLayer("Base").mayOnlyBeAccessedByLayers("Root", "Core", "Lang", "Library", "JUnit");

    @ArchTest
    public static final ArchRule domain_does_not_access_importer =
            noClasses().that().resideInAPackage("..core.domain..")
                    .should().accessClassesThat(belong_to_the_import_context());

    @ArchTest
    public static final ArchRule types_are_only_resolved_via_reflection_in_allowed_places =
            noClasses().that().resideOutsideOfPackage(THIRDPARTY_PACKAGE_IDENTIFIER)
                    .should().callMethodWhere(typeIsIllegallyResolvedViaReflection())
                    .as("no classes should illegally resolve classes via reflection");

    @ArchTest
    public static final ArchRules public_API_rules =
            ArchRules.in(PublicAPIRules.class);

    private static DescribedPredicate<JavaClass> belong_to_the_import_context() {
        return new DescribedPredicate<JavaClass>("belong to the import context") {
            @Override
            public boolean apply(JavaClass input) {
                return input.getPackageName().startsWith(ClassFileImporter.class.getPackage().getName())
                        && !input.getName().contains(DomainBuilders.class.getSimpleName());
            }
        };
    }

    private static DescribedPredicate<JavaCall<?>> typeIsIllegallyResolvedViaReflection() {
        DescribedPredicate<JavaCall<?>> explicitlyAllowedUsage =
                origin(is(annotatedWith(MayResolveTypesViaReflection.class)))
                        .or(contextIsAnnotatedWith(MayResolveTypesViaReflection.class)).forSubType();

        return classIsResolvedViaReflection().and(not(explicitlyAllowedUsage));
    }

    private static DescribedPredicate<JavaAccess<?>> contextIsAnnotatedWith(final Class<? extends Annotation> annotationType) {
        return origin(With.owner(withAnnotation(annotationType)));
    }

    private static DescribedPredicate<JavaClass> withAnnotation(final Class<? extends Annotation> annotationType) {
        return new DescribedPredicate<JavaClass>("annotated with @" + annotationType.getName()) {
            @Override
            public boolean apply(JavaClass input) {
                return input.isAnnotatedWith(annotationType)
                        || enclosingClassIsAnnotated(input);
            }

            private boolean enclosingClassIsAnnotated(JavaClass input) {
                return input.getEnclosingClass().isPresent() &&
                        input.getEnclosingClass().get().isAnnotatedWith(annotationType);
            }
        };
    }

    private static DescribedPredicate<JavaCall<?>> classIsResolvedViaReflection() {
        DescribedPredicate<JavaCall<?>> defaultClassForName =
                target(HasOwner.Functions.Get.<JavaClass>owner()
                        .is(equivalentTo(Class.class)))
                        .and(target(has(name("forName"))))
                        .forSubType();
        DescribedPredicate<JavaCall<?>> targetIsMarked =
                annotatedWith(ResolvesTypesViaReflection.class).onResultOf(Get.target());

        return defaultClassForName.or(targetIsMarked);
    }

    public static class ArchUnitProductionCode implements ImportOption {
        private static final Set<String> SOURCE_ROOTS = sourceRootsOf(ArchConfiguration.class, ArchUnitRunner.class);

        private static Set<String> sourceRootsOf(Class<?>... classes) {
            ImmutableSet.Builder<String> result = ImmutableSet.builder();
            for (Class<?> c : classes) {
                String classFile = "/" + c.getName().replace('.', '/') + ".class";
                String file = c.getResource(classFile).getFile();
                result.add(file.substring(0, file.indexOf(classFile)));
            }
            return result.build();
        }

        @Override
        public boolean includes(Location location) {
            boolean include = false;
            for (String sourceRoot : SOURCE_ROOTS) {
                if (location.contains(sourceRoot)) {
                    include = true;
                }
            }
            return include && DO_NOT_INCLUDE_TESTS.includes(location);
        }
    }
}
