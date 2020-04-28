package com.tngtech.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.DomainBuilders;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.ArchUnitArchitectureTest.THIRDPARTY_PACKAGE_IDENTIFIER;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ImporterRules {

    @ArchTest
    public static final ArchRule domain_does_not_access_importer =
            noClasses().that().resideInAPackage("..core.domain..")
                    .should().accessClassesThat(belong_to_the_import_context());

    @ArchTest
    public static final ArchRule ASM_type_is_only_accessed_within_JavaType_or_JavaTypeImporter =
            noClasses()
                    .that().resideOutsideOfPackage(THIRDPARTY_PACKAGE_IDENTIFIER)
                    .and(not(belongToAnyOf(JavaType.class)))
                    .and().doNotHaveFullyQualifiedName("com.tngtech.archunit.core.importer.JavaTypeImporter")
                    .should().dependOnClassesThat().haveNameMatching(".*\\.asm\\..*Type")
                    .as("org.objectweb.asm.Type should only be accessed within JavaType(Importer)")
                    .because("org.objectweb.asm.Type handles array types inconsistently (uses the canonical name instead of the class name), "
                            + "so the correct behavior is implemented only within JavaType");

    private static DescribedPredicate<JavaClass> belong_to_the_import_context() {
        return new DescribedPredicate<JavaClass>("belong to the import context") {
            @Override
            public boolean apply(JavaClass input) {
                return input.getPackageName().startsWith(ClassFileImporter.class.getPackage().getName())
                        && !input.getName().contains(DomainBuilders.class.getSimpleName());
            }
        };
    }
}
