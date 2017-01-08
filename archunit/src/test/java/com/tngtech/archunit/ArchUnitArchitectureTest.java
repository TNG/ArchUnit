package com.tngtech.archunit;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaStaticInitializer;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.conditions.CallPredicate;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.ClassFileImporter.PredefinedImportOption.DONT_INCLUDE_TESTS;
import static com.tngtech.archunit.core.JavaAccess.Predicates.withOrigin;
import static com.tngtech.archunit.core.JavaFieldAccess.Predicates.fieldAccessTarget;
import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.properties.HasName.Predicates.withNameMatching;
import static com.tngtech.archunit.lang.ArchRule.Definition.all;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callOrigin;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callTarget;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class ArchUnitArchitectureTest {
    private static final ClassFileImporter importer = new ClassFileImporter()
            .withImportOption(DONT_INCLUDE_TESTS);

    private static JavaClasses archUnitClasses;

    @BeforeClass
    public static void setUp() {
        archUnitClasses = importer.importPackages(ArchUnitArchitectureTest.class.getPackage().getName());
    }

    @Test
    public void layers_are_respected() {
        layeredArchitecture()
                .layer("Base").definedBy("com.tngtech.archunit.base..")
                .layer("Core").definedBy("com.tngtech.archunit.core..")
                .layer("Lang").definedBy("com.tngtech.archunit.lang..")
                .layer("Library").definedBy("com.tngtech.archunit.library..")

                .whereLayer("Library").mayNotBeAccessedByAnyLayer()
                .whereLayer("Lang").mayOnlyBeAccessedByLayers("Library")
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Lang", "Library")
                .whereLayer("Base").mayOnlyBeAccessedByLayers("Core", "Lang", "Library")

                .check(archUnitClasses);
    }

    @Test
    public void types_are_only_resolved_via_reflection_in_allowed_places() {
        all(classes()).should(notIllegallyResolveClassesViaReflection()).check(archUnitClasses);
    }

    private ArchCondition<JavaClass> notIllegallyResolveClassesViaReflection() {
        return never(callMethodWhere(targetResolvesTypesIllegallyViaReflection()))
                .and(never(illegallyAccessReflectFunction()))
                .as("not illegally resolve classes via reflection");
    }

    private DescribedPredicate<JavaCall<?>> targetResolvesTypesIllegallyViaReflection() {
        DescribedPredicate<JavaCall<?>> explicitlyAllowedUsage =
                callOrigin().is(annotatedWith(MayResolveTypesViaReflection.class))
                        .or(contextIsAnnotatedWith(MayResolveTypesViaReflection.class));

        return classIsResolvedViaReflection().and(not(explicitlyAllowedUsage));
    }

    private DescribedPredicate<JavaAccess<?>> contextIsAnnotatedWith(final Class<? extends Annotation> annotationType) {
        return withOrigin(With.owner(withAnnotation(annotationType)));
    }

    private DescribedPredicate<JavaClass> withAnnotation(final Class<? extends Annotation> annotationType) {
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

    private DescribedPredicate<JavaCall<?>> classIsResolvedViaReflection() {
        CallPredicate defaultClassForName = callTarget().isDeclaredIn(Class.class).hasName("forName");
        DescribedPredicate<JavaCall<?>> targetIsMarked =
                annotatedWith(ResolvesTypesViaReflection.class).onResultOf(Get.target());

        return defaultClassForName.or(targetIsMarked);
    }

    private ArchCondition<JavaClass> illegallyAccessReflectFunction() {
        DescribedPredicate<JavaFieldAccess> targetIsReflectFunction = fieldAccessTarget(JavaClass.class, "REFLECT");
        DescribedPredicate<JavaAccess<?>> withForbiddenOrigin =
                not(withOrigin(withNameMatching(JavaStaticInitializer.STATIC_INITIALIZER_NAME)))
                        .and(not(withOrigin(annotatedWith(MayResolveTypesViaReflection.class))))
                        .and(not(withOrigin(With.<JavaClass>owner(annotatedWith(MayResolveTypesViaReflection.class)))));

        return accessFieldWhere(targetIsReflectFunction.and(withForbiddenOrigin));
    }
}
