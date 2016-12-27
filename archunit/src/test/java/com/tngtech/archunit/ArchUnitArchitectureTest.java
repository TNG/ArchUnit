package com.tngtech.archunit;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaStaticInitializer;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ReflectionUtils;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.conditions.CallPredicate;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.ClassFileImporter.PredefinedImportOption.DONT_INCLUDE_TESTS;
import static com.tngtech.archunit.core.DescribedPredicate.not;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.accessOrigin;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callOrigin;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callTarget;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.declaredIn;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.named;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndNameAre;

public class ArchUnitArchitectureTest {
    private static final ClassFileImporter importer = new ClassFileImporter()
            .withImportOption(DONT_INCLUDE_TESTS);

    private static JavaClasses archUnitClasses;

    @BeforeClass
    public static void setUp() {
        archUnitClasses = importer.importPackages(ArchUnitArchitectureTest.class.getPackage().getName());
    }

    @Test
    @Ignore
    public void types_are_only_resolved_via_reflection_in_allowed_places() {
        all(archUnitClasses).should(notIllegallyResolveClassesViaReflection());
    }

    private ArchCondition<JavaClass> notIllegallyResolveClassesViaReflection() {
        return never(callMethodWhere(targetResolvesTypesIllegallyViaReflection()))
                .and(never(illegallyAccessReflectFunction()))
                .as("not illegally resolve classes via reflection");
    }

    private DescribedPredicate<JavaCall<?>> targetResolvesTypesIllegallyViaReflection() {
        DescribedPredicate<JavaCall<?>> explicitlyAllowedUsage =
                callOrigin().is(annotatedWith(MayResolveTypesViaReflection.class))
                        .or(callOrigin().isDeclaredIn(annotatedWith(MayResolveTypesViaReflection.class)));

        return classIsResolvedViaReflection().and(not(explicitlyAllowedUsage));
    }

    private DescribedPredicate<JavaCall<?>> classIsResolvedViaReflection() {
        CallPredicate defaultClassForName = callTarget().is(Class.class, "forName", String.class);
        DescribedPredicate<JavaCall<?>> reflectionUtilsClassForName =
                callTarget().is(ReflectionUtils.class, "classForName", String.class);
        DescribedPredicate<JavaCall<?>> reflectionUtilsTryGetClassForName =
                callTarget().is(ReflectionUtils.class, "tryGetClassForName", String.class);
        DescribedPredicate<JavaCall<?>> javaClassReflect = callTarget().is(JavaClass.class, "reflect");

        return defaultClassForName
                .or(reflectionUtilsClassForName)
                .or(reflectionUtilsTryGetClassForName)
                .or(javaClassReflect);
    }

    private ArchCondition<JavaClass> illegallyAccessReflectFunction() {
        DescribedPredicate<JavaFieldAccess> targetIsReflectFunction = ownerAndNameAre(JavaClass.class, "REFLECT");
        DescribedPredicate<JavaFieldAccess> notAllowedOrigin =
                not(accessOrigin(named(JavaStaticInitializer.STATIC_INITIALIZER_NAME)))
                        .and(not(accessOrigin(annotatedWith(MayResolveTypesViaReflection.class))))
                        .and(not(accessOrigin(declaredIn(annotatedWith(MayResolveTypesViaReflection.class)))));

        return accessFieldWhere(targetIsReflectFunction.and(notAllowedOrigin));
    }
}
