package com.tngtech.archunit;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaStaticInitializer;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.conditions.CallPredicate;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.tngtech.archunit.core.ClassFileImporter.PredefinedImportOption.DONT_INCLUDE_TESTS;
import static com.tngtech.archunit.core.DescribedPredicate.not;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.ArchRule.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.accessOrigin;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callOrigin;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callTarget;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndNameAre;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerIs;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.withName;

public class ArchUnitArchitectureTest {
    private static final ClassFileImporter importer = new ClassFileImporter()
            .withImportOption(DONT_INCLUDE_TESTS);

    private static JavaClasses archUnitClasses;

    @BeforeClass
    public static void setUp() {
        archUnitClasses = importer.importPackages(ArchUnitArchitectureTest.class.getPackage().getName());
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

    private DescribedPredicate<JavaCall<?>> contextIsAnnotatedWith(final Class<? extends Annotation> annotationType) {
        return callOrigin(ownerIs(new DescribedPredicate<JavaClass>(
                "annotated with @" + annotationType.getName()) {

            @Override
            public boolean apply(JavaClass input) {
                return input.isAnnotatedWith(annotationType)
                        || enclosingClassIsAnnotated(input);
            }

            private boolean enclosingClassIsAnnotated(JavaClass input) {
                return input.getEnclosingClass().isPresent() &&
                        input.getEnclosingClass().get().isAnnotatedWith(annotationType);
            }
        }));
    }

    private DescribedPredicate<JavaCall<?>> classIsResolvedViaReflection() {
        CallPredicate defaultClassForName = callTarget().isDeclaredIn(Class.class).hasName("forName");
        DescribedPredicate<JavaCall<?>> targetIsMarked = callTarget(annotatedWith(ResolvesTypesViaReflection.class));

        return defaultClassForName.or(targetIsMarked);
    }

    private ArchCondition<JavaClass> illegallyAccessReflectFunction() {
        DescribedPredicate<JavaFieldAccess> targetIsReflectFunction = ownerAndNameAre(JavaClass.class, "REFLECT");
        DescribedPredicate<JavaFieldAccess> notAllowedOrigin =
                not(accessOrigin(withName(JavaStaticInitializer.STATIC_INITIALIZER_NAME)))
                        .and(not(accessOrigin(annotatedWith(MayResolveTypesViaReflection.class))))
                        .and(not(accessOrigin(ownerIs(annotatedWith(MayResolveTypesViaReflection.class)))));

        return accessFieldWhere(targetIsReflectFunction.and(notAllowedOrigin));
    }
}
