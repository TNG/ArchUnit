package com.tngtech.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.DomainBuilders;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.ArchUnitArchitectureTest.THIRDPARTY_PACKAGE_IDENTIFIER;
import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ImporterRules {

    @ArchTest
    public static final ArchRule domain_does_not_access_importer =
            noClasses().that().resideInAPackage("..core.domain..")
                    .should().dependOnClassesThat(belong_to_the_import_context());

    @ArchTest
    public static final ArchRule asm_is_only_used_in_importer_or_JavaClassDescriptor =
            noClasses()
                    .that(
                            resideOutsideOfPackage("..core.importer..")
                                    .and(doNot(belongToAnyOf(JavaClassDescriptor.class
                                            // Conceptually there are also dependencies from JavaModifier to ASM.
                                            // However, at the moment all those are inlined by the compiler (primitives).
                                            // Whenever we get the chance to break the public API
                                            // we should remove the dependencies from JavaModifier to ASM.
                                            // Those dependencies crept in by accident at some point because the design was convenient.
                                    )))
                    )
                    .should().dependOnClassesThat().resideInAPackage("org.objectweb..");

    @ArchTest
    public static final ArchRule ASM_type_is_only_accessed_within_JavaClassDescriptor_or_JavaClassDescriptorImporter =
            noClasses()
                    .that().resideOutsideOfPackage(THIRDPARTY_PACKAGE_IDENTIFIER)
                    .and(not(belongToAnyOf(JavaClassDescriptor.class)))
                    .and().doNotHaveFullyQualifiedName("com.tngtech.archunit.core.importer.JavaClassDescriptorImporter")
                    .should().dependOnClassesThat().haveNameMatching(".*\\.asm\\..*Type")
                    .as("org.objectweb.asm.Type should only be accessed within JavaClassDescriptor(Importer)")
                    .because("org.objectweb.asm.Type handles array types inconsistently (uses the canonical name instead of the class name), "
                            + "so the correct behavior is implemented only within JavaClassDescriptor");

    private static DescribedPredicate<JavaClass> belong_to_the_import_context() {
        return new DescribedPredicate<JavaClass>("belong to the import context") {
            @Override
            public boolean test(JavaClass input) {
                return input.getPackageName().startsWith(ClassFileImporter.class.getPackage().getName())
                        && !input.getName().contains(DomainBuilders.class.getSimpleName());
            }
        };
    }
}
