package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.SomeBusinessInterface;
import com.tngtech.archunit.example.layers.persistence.first.dao.SomeDao;
import com.tngtech.archunit.example.layers.service.impl.SomeInterfacePlacedInTheWrongPackage;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Category(Example.class)
public class InterfaceRulesTest {
    private final JavaClasses classes = new ClassFileImporter().importClasses(
            SomeBusinessInterface.class,
            SomeDao.class,
            SomeInterfacePlacedInTheWrongPackage.class
    );

    @Test
    public void interfaces_should_not_have_names_ending_with_the_word_interface() {
        noClasses().that().areInterfaces().should().haveNameMatching(".*Interface").check(classes);
    }

    @Test
    public void interfaces_should_not_have_simple_class_names_containing_the_word_interface() {
        noClasses().that().areInterfaces().should().haveSimpleNameContaining("Interface").check(classes);
    }

    @Test
    public void interfaces_must_not_be_placed_in_implementation_packages() {
        noClasses().that().resideInAPackage("..impl..").should().beInterfaces().check(classes);
    }
}
