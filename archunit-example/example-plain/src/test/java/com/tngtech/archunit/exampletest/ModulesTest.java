package com.tngtech.archunit.exampletest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.AppModule;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;
import com.tngtech.archunit.library.modules.syntax.DescriptorFunction;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Category(Example.class)
public class ModulesTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example");

    /**
     * This example demonstrates how to derive modules from a package pattern.
     * The `..` stands for arbitrary many packages and the `(*)` captures one specific subpackage name within the
     * package tree.
     */
    @Test
    public void modules_should_respect_their_declared_dependencies__use_package_API() {
        modules()
                .definedByPackages("..shopping.(*)..")
                .should(respectAllowedDependencies(new HashMap<String, Collection<String>>() {{
                    put("catalog", singletonList("product"));
                    put("customer", singletonList("address"));
                    put("importer", asList("catalog", "xml"));
                    put("order", asList("customer", "product"));
                }}))
                .check(classes);
    }

    /**
     * This example demonstrates how to easily derive modules from classes annotated with a certain annotation.
     * Within the example those are simply package-info files which denote the root of the modules by
     * being annotated with @AppModule.
     */
    @Test
    public void modules_should_respect_their_declared_dependencies__use_annotation_API() {
        modules()
                .definedByAnnotation(AppModule.class)
                .should(respectTheirDeclaredDependenciesWithin("..example.."))
                .check(classes);
    }

    /**
     * This example demonstrates how to use the slightly more generic root class API to define modules.
     * While the result in this example is the same as the above, this API in general can be used to
     * use arbitrary classes as roots of modules.
     * For example if there is always a central interface denoted in some way,
     * the modules could be derived from these interfaces.
     */
    @Test
    public void modules_should_respect_their_declared_dependencies__use_root_class_API() {
        modules()
                .definedByRootClasses(
                        DescribedPredicate.describe("annotated with @" + AppModule.class.getSimpleName(), (JavaClass rootClass) ->
                                rootClass.isAnnotatedWith(AppModule.class))
                )
                .derivingModuleFromRootClassBy(
                        DescribedFunction.describe("annotation @" + AppModule.class.getSimpleName(), (JavaClass rootClass) -> {
                            AppModule module = rootClass.getAnnotationOfType(AppModule.class);
                            return new AnnotationDescriptor<>(module.name(), module);
                        })
                )
                .should(respectTheirDeclaredDependenciesWithin("..example.."))
                .check(classes);
    }

    /**
     * This example demonstrates how to use the generic API to define modules.
     * The result in this example again is the same as the above, however in general the generic API
     * allows to derive modules in a completely customizable way.
     */
    @Test
    public void modules_should_respect_their_declared_dependencies__use_generic_API() {
        modules()
                .definedBy(identifierFromModulesAnnotation())
                .derivingModule(fromModulesAnnotation())
                .should(respectTheirDeclaredDependenciesWithin("..example.."))
                .check(classes);
    }

    private static IdentifierFromAnnotation identifierFromModulesAnnotation() {
        return new IdentifierFromAnnotation();
    }

    private static DescriptorFunction<AnnotationDescriptor<AppModule>> fromModulesAnnotation() {
        return DescriptorFunction.describe(String.format("from @%s(name)", AppModule.class.getSimpleName()),
                (ArchModule.Identifier identifier, Set<JavaClass> containedClasses) -> {
                    JavaClass rootClass = containedClasses.stream().filter(it -> it.isAnnotatedWith(AppModule.class)).findFirst().get();
                    AppModule module = rootClass.getAnnotationOfType(AppModule.class);
                    return new AnnotationDescriptor<>(module.name(), module);
                });
    }

    private static RespectAllowedDependenciesCondition respectAllowedDependencies(Map<String, Collection<String>> allowedDependencies) {
        return new RespectAllowedDependenciesCondition(allowedDependencies);
    }

    private static DeclaredDependenciesCondition respectTheirDeclaredDependenciesWithin(String applicationRootPackageIdentifier) {
        return new DeclaredDependenciesCondition(applicationRootPackageIdentifier);
    }

    private static class RespectAllowedDependenciesCondition extends ArchCondition<ArchModule<?>> {
        private final Map<String, Collection<String>> allowedDependencies;

        RespectAllowedDependenciesCondition(Map<String, Collection<String>> allowedDependencies) {
            super("respect allowed dependencies %s", format(allowedDependencies));
            this.allowedDependencies = allowedDependencies;
        }

        private static String format(Map<String, Collection<String>> allowedDependencies) {
            return allowedDependencies.entrySet().stream()
                    .map(originToTargets -> originToTargets.getKey() + "->" + originToTargets.getValue())
                    .sorted()
                    .collect(joining(" "));
        }

        @Override
        public void check(ArchModule<?> module, ConditionEvents events) {
            module.getModuleDependenciesFromSelf().stream()
                    .filter(this::isForbidden)
                    .forEach(it -> events.add(SimpleConditionEvent.violated(module, it.getDescription())));
        }

        private boolean isForbidden(ModuleDependency<?> moduleDependency) {
            String originName = moduleDependency.getOrigin().getName();
            String targetName = moduleDependency.getTarget().getName();

            return !allowedDependencies.containsKey(originName) || !allowedDependencies.get(originName).contains(targetName);
        }
    }

    private static class DeclaredDependenciesCondition extends ArchCondition<ArchModule<AnnotationDescriptor<AppModule>>> {
        private final PackageMatcher applicationRootPackageMatcher;

        DeclaredDependenciesCondition(String applicationRootPackageIdentifier) {
            super("respect their declared dependencies within %s", applicationRootPackageIdentifier);
            this.applicationRootPackageMatcher = PackageMatcher.of(applicationRootPackageIdentifier);
        }

        @Override
        public void check(ArchModule<AnnotationDescriptor<AppModule>> module, ConditionEvents events) {
            Set<ModuleDependency<AnnotationDescriptor<AppModule>>> actualDependencies = module.getModuleDependenciesFromSelf();
            Set<String> allowedDependencyTargets = Arrays.stream(module.getDescriptor().getAnnotation().allowedDependencies()).collect(toSet());

            actualDependencies.stream()
                    .filter(it -> !allowedDependencyTargets.contains(it.getTarget().getName()))
                    .forEach(it -> events.add(violated(it, it.getDescription())));

            module.getUndefinedDependencies().stream()
                    .filter(it -> !it.getTargetClass().isEquivalentTo(AppModule.class))
                    .filter(it -> applicationRootPackageMatcher.matches(it.getTargetClass().getPackageName()))
                    .forEach(it -> events.add(violated(it, "Dependency not contained in any module: " + it.getDescription())));
        }
    }

    private static class IdentifierFromAnnotation extends DescribedFunction<JavaClass, ArchModule.Identifier> {
        IdentifierFromAnnotation() {
            super("root classes with annotation @" + AppModule.class.getSimpleName());
        }

        @Override
        public ArchModule.Identifier apply(JavaClass javaClass) {
            return getIdentifierOfPackage(javaClass.getPackage());
        }

        private ArchModule.Identifier getIdentifierOfPackage(JavaPackage javaPackage) {
            Optional<ArchModule.Identifier> identifierInCurrentPackage = javaPackage.getClasses().stream()
                    .filter(it -> it.isAnnotatedWith(AppModule.class))
                    .findFirst()
                    .map(annotatedClassInPackage -> ArchModule.Identifier.from(annotatedClassInPackage.getAnnotationOfType(AppModule.class).name()));

            return identifierInCurrentPackage.orElseGet(identifierInParentPackageOf(javaPackage));
        }

        private Supplier<ArchModule.Identifier> identifierInParentPackageOf(JavaPackage javaPackage) {
            return () -> javaPackage.getParent()
                    .map(this::getIdentifierOfPackage)
                    .orElseGet(ArchModule.Identifier::ignore);
        }
    }
}
