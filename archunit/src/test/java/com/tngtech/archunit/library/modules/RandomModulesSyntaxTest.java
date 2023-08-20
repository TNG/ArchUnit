package com.tngtech.archunit.library.modules;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.library.modules.syntax.AllowedModuleDependencies;
import com.tngtech.archunit.library.modules.syntax.DescriptorFunction;
import com.tngtech.archunit.library.modules.syntax.GivenModules;
import com.tngtech.archunit.library.modules.syntax.ModuleDependencyScope;
import com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition;
import com.tngtech.archunit.testutil.syntax.Parameter;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxSeed;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxTestBase;
import com.tngtech.archunit.testutil.syntax.SingleParameterProvider;
import com.tngtech.java.junit.dataprovider.DataProvider;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.testutil.syntax.MethodChoiceStrategy.chooseAllArchUnitSyntaxMethods;
import static java.util.stream.Collectors.toList;

public class RandomModulesSyntaxTest extends RandomSyntaxTestBase {
    @DataProvider
    public static List<List<?>> random_rules() {
        return createRandomRulesForSeeds(
                new RandomSyntaxSeed<>(
                        givenModulesClass(),
                        ModuleRuleDefinition.modules().definedByPackages("..test.(*).."),
                        "modules defined by packages '..test.(*)..'"),
                new RandomSyntaxSeed<>(
                        givenModulesClass(),
                        ModuleRuleDefinition.modules().definedByAnnotation(RandomSyntaxModule.class),
                        "modules defined by annotation @" + RandomSyntaxModule.class.getSimpleName()),
                new RandomSyntaxSeed<>(
                        givenModulesClass(),
                        ModuleRuleDefinition.modules().definedByAnnotation(RandomSyntaxModule.class, RandomSyntaxModule::name),
                        "modules defined by annotation @" + RandomSyntaxModule.class.getSimpleName()),
                new RandomSyntaxSeed<>(
                        givenModulesClass(),
                        ModuleRuleDefinition.modules()
                                .definedByRootClasses(DescribedPredicate.describe("some predicate", alwaysTrue()))
                                .derivingModuleFromRootClassBy(DescribedFunction.describe("some function", it -> ArchModule.Descriptor.create("irrelevant"))),
                        "modules defined by root classes some predicate deriving module from root class by some function"),
                new RandomSyntaxSeed<>(
                        givenModulesClass(),
                        ModuleRuleDefinition.modules()
                                .definedBy(DescribedFunction.describe("some function", it -> ArchModule.Identifier.ignore()))
                                .derivingModule(DescriptorFunction.describe("some other function", (__, ___) -> ArchModule.Descriptor.create("irrelevant"))),
                        "modules defined by some function deriving module some other function")
        );
    }

    @SafeVarargs
    private static List<List<?>> createRandomRulesForSeeds(RandomSyntaxSeed<GivenModules<?>>... seeds) {
        return Arrays.stream(seeds)
                .map(seed -> RandomSyntaxTestBase.createRandomRules(
                        RandomRulesBlueprint
                                .seed(seed)
                                .methodChoiceStrategy(chooseAllArchUnitSyntaxMethods().exceptMethodsWithName("ignoreDependency"))
                                .parameterProviders(
                                        new SingleParameterProvider(ModuleDependencyScope.class) {
                                            @Override
                                            public Parameter get(String methodName, TypeToken<?> type) {
                                                ModuleDependencyScope dependencyScope = randomElement(
                                                        ModuleDependencyScope.consideringAllDependencies(),
                                                        ModuleDependencyScope.consideringOnlyDependenciesBetweenModules(),
                                                        ModuleDependencyScope.consideringOnlyDependenciesInAnyPackage("..test..")
                                                );
                                                return new Parameter(dependencyScope, dependencyScope.getDescription());
                                            }

                                            @SafeVarargs
                                            private final <T> T randomElement(T... elements) {
                                                return elements[random.nextInt(elements.length)];
                                            }
                                        },
                                        new SingleParameterProvider(AllowedModuleDependencies.class) {
                                            @Override
                                            public Parameter get(String methodName, TypeToken<?> type) {
                                                return new Parameter(
                                                        AllowedModuleDependencies.allow()
                                                                .fromModule("Module One").toModules("Module Two", "Module Three")
                                                                .fromModule("Module Two").toModules("Module Three"),
                                                        "{ Module One -> [Module Two, Module Three], Module Two -> [Module Three] }"
                                                );
                                            }
                                        })
                                .descriptionReplacements(new ReplaceEverythingSoFar("as '([^']+)'.*", "$1")))
                )
                .flatMap(Collection::stream)
                .collect(toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<GivenModules<?>> givenModulesClass() {
        return (Class) GivenModules.class;
    }

    private @interface RandomSyntaxModule {
        String name();
    }
}
