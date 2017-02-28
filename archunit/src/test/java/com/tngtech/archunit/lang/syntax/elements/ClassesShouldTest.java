package com.tngtech.archunit.lang.syntax.elements;

import java.util.regex.Pattern;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassesShouldTest {
    @DataProvider
    public static Object[][] beNamed_rules() {
        return $$(
                $(classes().should().beNamed(RightNamedClass.class.getName())),
                $(classes().should(ArchConditions.beNamed(RightNamedClass.class.getName())))
        );
    }

    @Test
    @UseDataProvider("beNamed_rules")
    public void beNamed(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(result.getFailureReport().toString())
                .contains(String.format("classes should be named '%s'", RightNamedClass.class.getName()))
                .contains(String.format("%s is not named '%s'", WrongNamedClass.class.getName(), RightNamedClass.class.getName()))
                .doesNotContain(String.format("%s is", RightNamedClass.class.getName()));
    }

    @DataProvider
    public static Object[][] accessField_rules() {
        return $$(
                $(classes().should().getField(ClassWithField.class, "field"), "get", "gets"),
                $(classes().should(ArchConditions.getField(ClassWithField.class, "field")), "get", "gets"),
                $(classes().should().getField(ClassWithField.class.getName(), "field"), "get", "gets"),
                $(classes().should(ArchConditions.getField(ClassWithField.class.getName(), "field")), "get", "gets"),
                $(classes().should().setField(ClassWithField.class, "field"), "set", "sets"),
                $(classes().should(ArchConditions.setField(ClassWithField.class, "field")), "set", "sets"),
                $(classes().should().setField(ClassWithField.class.getName(), "field"), "set", "sets"),
                $(classes().should(ArchConditions.setField(ClassWithField.class.getName(), "field")), "set", "sets"),
                $(classes().should().accessField(ClassWithField.class, "field"), "access", "accesses"),
                $(classes().should(ArchConditions.accessField(ClassWithField.class, "field")), "access", "accesses"),
                $(classes().should().accessField(ClassWithField.class.getName(), "field"), "access", "accesses"),
                $(classes().should(ArchConditions.accessField(ClassWithField.class.getName(), "field")), "access", "accesses")
        );
    }

    @Test
    @UseDataProvider("accessField_rules")
    public void accessField(ArchRule rule, String accessTypePlural, String accessTypeSingular) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithField.class, ClassAccessingField.class, ClassAccessingWrongField.class));

        assertThat(result.getFailureReport().toString().replaceAll("\\s+", " "))
                .contains(String.format("classes should %s field %s.%s",
                        accessTypePlural, ClassWithField.class.getSimpleName(), "field"))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongField.class, accessTypeSingular,
                        ClassAccessingField.class, "classWithField"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingField.class, accessTypeSingular,
                        ClassWithField.class, "field"));
    }

    @DataProvider
    public static Object[][] accessFieldWhere_rules() {
        return $$(
                $(classes().should().getFieldWhere(targetIs(ClassWithField.class)), "get", "gets"),
                $(classes().should(ArchConditions.getFieldWhere(targetIs(ClassWithField.class))), "get", "gets"),
                $(classes().should().setFieldWhere(targetIs(ClassWithField.class)), "set", "sets"),
                $(classes().should(ArchConditions.setFieldWhere(targetIs(ClassWithField.class))), "set", "sets"),
                $(classes().should().accessFieldWhere(targetIs(ClassWithField.class)), "access", "accesses"),
                $(classes().should(ArchConditions.accessFieldWhere(targetIs(ClassWithField.class))), "access", "accesses")
        );
    }

    private static DescribedPredicate<JavaFieldAccess> targetIs(final Class<?> targetType) {
        return new DescribedPredicate<JavaFieldAccess>("target owner is " + targetType.getSimpleName()) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return input.getTargetOwner().isEquivalentTo(targetType);
            }
        };
    }

    @Test
    @UseDataProvider("accessFieldWhere_rules")
    public void accessFieldWhere(ArchRule rule, String accessTypePlural, String accessTypeSingular) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithField.class, ClassAccessingField.class, ClassAccessingWrongField.class));

        assertThat(result.getFailureReport().toString().replaceAll("\\s+", " "))
                .contains(String.format("classes should %s field where target owner is %s",
                        accessTypePlural, ClassWithField.class.getSimpleName()))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongField.class, accessTypeSingular,
                        ClassAccessingField.class, "classWithField"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingField.class, accessTypeSingular,
                        ClassWithField.class, "field"));
    }

    private Pattern accessesFieldRegex(Class<?> origin, String accessType, Class<?> targetClass, String fieldName) {
        return Pattern.compile(String.format(".*%s.*%s field.*%s\\.%s.*",
                quote(origin.getName()), accessType, targetClass.getSimpleName(), fieldName), MULTILINE);
    }

    private static class RightNamedClass {
    }

    private static class WrongNamedClass {
    }

    private static class ClassWithField {
        String field;
    }

    private static class ClassAccessingField {
        ClassWithField classWithField;

        String access() {
            classWithField.field = "new";
            return classWithField.field;
        }
    }

    private static class ClassAccessingWrongField {
        ClassAccessingField classAccessingField;

        ClassWithField evilAccess() {
            classAccessingField.classWithField = null;
            return classAccessingField.classWithField;
        }
    }
}
