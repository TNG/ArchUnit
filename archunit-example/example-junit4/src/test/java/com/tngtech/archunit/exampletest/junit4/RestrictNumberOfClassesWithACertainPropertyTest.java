package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.example.layers.SomeBusinessInterface;
import com.tngtech.archunit.example.layers.SomeOtherBusinessInterface;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.lessThanOrEqualTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class RestrictNumberOfClassesWithACertainPropertyTest {
    @ArchTest
    public static final ArchRule no_new_classes_should_implement_SomeBusinessInterface =
            classes().that().implement(SomeBusinessInterface.class)
                    .should().containNumberOfElements(lessThanOrEqualTo(1))
                    .because("from now on new classes should implement " + SomeOtherBusinessInterface.class.getName());
}
