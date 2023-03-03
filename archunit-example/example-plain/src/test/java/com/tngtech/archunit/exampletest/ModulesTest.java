package com.tngtech.archunit.exampletest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.AppModule;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;
import com.tngtech.archunit.library.modules.syntax.DescriptorFunction;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.library.modules.syntax.AllowedModuleDependencies.allow;
import static com.tngtech.archunit.library.modules.syntax.ModuleDependencyScope.consideringOnlyDependenciesInAnyPackage;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

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
                .should().respectTheirAllowedDependencies(
                        allow()
                                .fromModule("catalog").toModules("product")
                                .fromModule("customer").toModules("address")
                                .fromModule("importer").toModules("catalog", "xml")
                                .fromModule("order").toModules("customer", "product"),
                        consideringOnlyDependenciesInAnyPackage("..example.."))
                .ignoreDependency(alwaysTrue(), equivalentTo(AppModule.class))
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
                .should().respectTheirAllowedDependencies(
                        declaredByDescriptorAnnotation(),
                        consideringOnlyDependenciesInAnyPackage("..example..")
                )
                .ignoreDependency(alwaysTrue(), equivalentTo(AppModule.class))
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
                .should().respectTheirAllowedDependencies(
                        declaredByDescriptorAnnotation(),
                        consideringOnlyDependenciesInAnyPackage("..example..")
                )
                .ignoreDependency(alwaysTrue(), equivalentTo(AppModule.class))
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
                .should().respectTheirAllowedDependencies(
                        declaredByDescriptorAnnotation(),
                        consideringOnlyDependenciesInAnyPackage("..example..")
                )
                .ignoreDependency(alwaysTrue(), equivalentTo(AppModule.class))
                .check(classes);
    }

    /**
     * This example demonstrates how to check for cyclic dependencies between modules.
     */
    @Test
    public void modules_should_be_free_of_cycles() {
        modules()
                .definedByAnnotation(AppModule.class)
                .should().beFreeOfCycles()
                .check(classes);
    }

    private static DescribedPredicate<ModuleDependency<AnnotationDescriptor<AppModule>>> declaredByDescriptorAnnotation() {
        return DescribedPredicate.describe("declared by descriptor annotation", moduleDependency -> {
            AppModule descriptor = moduleDependency.getOrigin().getDescriptor().getAnnotation();
            List<String> allowedDependencies = stream(descriptor.allowedDependencies()).collect(toList());
            return allowedDependencies.contains(moduleDependency.getTarget().getName());
        });
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
