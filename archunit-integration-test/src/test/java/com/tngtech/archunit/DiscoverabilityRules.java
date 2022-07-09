package com.tngtech.archunit;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.testutil.ReflectionTestUtils;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.classForName;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(
        packagesOf = ArchUnitArchitectureTest.class,
        importOptions = ArchUnitArchitectureTest.ArchUnitProductionCode.class)
public class DiscoverabilityRules {
    @ArchTest
    public static final ArchRule DescribedPredicates_are_hooked_into_central_entry_point =
            ArchRuleDefinition.classes()
                    .that().resideInAPackage("..core.domain..")
                    .and().areNotInterfaces()
                    .and(definePredicates())
                    .should(haveAllPredicatesHookedIntoCentralPredicatesEntryPoint());

    private static DescribedPredicate<JavaClass> definePredicates() {
        return DescribedPredicate.describe("define predicates", clazz ->
                clazz.getClassHierarchy().stream()
                        .anyMatch(classOrAncestor -> classExists(classOrAncestor.getName() + "$Predicates")));
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static ArchCondition<JavaClass> haveAllPredicatesHookedIntoCentralPredicatesEntryPoint() {
        return new ArchCondition<JavaClass>("have all predicates hooked into central predicates entry point") {
            private final Map<JavaClass, Class<?>> domainPredicatesToEntryPointPredicates = new HashMap<>();

            @Override
            public void init(Collection<JavaClass> allClasses) {
                allClasses.forEach(javaClass ->
                        domainPredicatesToEntryPointPredicates
                                .put(javaClass, classForName(ArchUnit.Predicates.class.getName() + "$For" + javaClass.getSimpleName())));
            }

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Set<? extends Class<?>> predicateClassesInHierarchy = javaClass.getClassHierarchy().stream()
                        .map(c -> c.getName() + "$Predicates")
                        .filter(DiscoverabilityRules::classExists)
                        .map(ReflectionTestUtils::classForName)
                        .collect(toSet());

                Class<?> entryPointPredicates = domainPredicatesToEntryPointPredicates.get(javaClass);

                Set<Method> predicateMethods = predicateClassesInHierarchy.stream().flatMap(c -> stream(c.getMethods())).collect(toSet());
                predicateMethods.stream()
                        .filter(method -> methodDoesNotExist(entryPointPredicates, method))
                        .forEach(method -> events.add(violated(javaClass, method + " does not exist in entry point " + entryPointPredicates.getName())));
            }

            private boolean methodDoesNotExist(Class<?> entryPointPredicates, Method method) {
                try {
                    entryPointPredicates.getMethod(method.getName(), method.getParameterTypes());
                    return false;
                } catch (Exception e) {
                    return true;
                }
            }
        };
    }
}
