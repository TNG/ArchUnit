package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.SomeBusinessInterface;
import com.tngtech.archunit.example.layers.SomeOtherBusinessInterface;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.base.DescribedPredicate.lessThanOrEqualTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
public class RestrictNumberOfClassesWithACertainPropertyTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example.layers");

    @Test
    public void no_new_classes_should_implement_SomeBusinessInterface() {
        classes().that().implement(SomeBusinessInterface.class)
                .should().containNumberOfElements(lessThanOrEqualTo(1))
                .because("from now on new classes should implement " + SomeOtherBusinessInterface.class.getName())
                .check(classes);
    }
}
