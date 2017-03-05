package com.tngtech.archunit.lang.syntax.elements;

import java.util.regex.Pattern;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaMethodCall;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.JavaMethodCall.Predicates.target;
import static com.tngtech.archunit.core.TestUtils.importClasses;
import static com.tngtech.archunit.core.properties.HasOwner.Predicates.With.owner;
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
                $(classes().should().getFieldWhere(accessTargetIs(ClassWithField.class)), "get", "gets"),
                $(classes().should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))), "get", "gets"),
                $(classes().should().setFieldWhere(accessTargetIs(ClassWithField.class)), "set", "sets"),
                $(classes().should(ArchConditions.setFieldWhere(accessTargetIs(ClassWithField.class))), "set", "sets"),
                $(classes().should().accessFieldWhere(accessTargetIs(ClassWithField.class)), "access", "accesses"),
                $(classes().should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))), "access", "accesses")
        );
    }

    private static DescribedPredicate<JavaFieldAccess> accessTargetIs(final Class<?> targetType) {
        return JavaFieldAccess.Predicates.target(owner(type(targetType))).as("target owner is " + targetType.getSimpleName());
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

    @DataProvider
    public static Object[][] callMethod_rules() {
        return $$(
                $(classes().should().callMethod(ClassWithMethod.class, "method", String.class)),
                $(classes().should(ArchConditions.callMethod(ClassWithMethod.class, "method", String.class))),
                $(classes().should().callMethod(ClassWithMethod.class.getName(), "method", String.class.getName())),
                $(classes().should(ArchConditions.callMethod(ClassWithMethod.class.getName(), "method", String.class.getName())))
        );
    }

    @Test
    @UseDataProvider("callMethod_rules")
    public void callMethod(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithMethod.class, ClassCallingMethod.class, ClassCallingWrongMethod.class));

        assertThat(result.getFailureReport().toString().replaceAll("\\s+", " "))
                .contains(String.format("classes should call method %s.%s(%s)",
                        ClassWithMethod.class.getSimpleName(), "method", String.class.getSimpleName()))
                .containsPattern(callMethodRegex(
                        ClassCallingWrongMethod.class,
                        ClassCallingMethod.class, "call"))
                .doesNotMatch(callMethodRegex(
                        ClassCallingMethod.class,
                        ClassWithMethod.class, "method", String.class));
    }

    @DataProvider
    public static Object[][] callMethodWhere_rules() {
        return $$(
                $(classes().should().callMethodWhere(callTargetIs(ClassWithMethod.class))),
                $(classes().should(ArchConditions.callMethodWhere(callTargetIs(ClassWithMethod.class))))
        );
    }

    private static DescribedPredicate<JavaMethodCall> callTargetIs(Class<?> type) {
        return target(owner(type(type))).as("target is " + type.getSimpleName());
    }

    @Test
    @UseDataProvider("callMethodWhere_rules")
    public void callMethodWhere(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithMethod.class, ClassCallingMethod.class, ClassCallingWrongMethod.class));

        assertThat(result.getFailureReport().toString().replaceAll("\\s+", " "))
                .contains(String.format("classes should call method where target is %s",
                        ClassWithMethod.class.getSimpleName()))
                .containsPattern(callMethodRegex(
                        ClassCallingWrongMethod.class,
                        ClassCallingMethod.class, "call"))
                .doesNotMatch(callMethodRegex(
                        ClassCallingMethod.class,
                        ClassWithMethod.class, "method", String.class));
    }

    private Pattern accessesFieldRegex(Class<?> origin, String accessType, Class<?> targetClass, String fieldName) {
        return Pattern.compile(String.format(".*%s.*%s field.*%s\\.%s.*",
                quote(origin.getName()), accessType, quote(targetClass.getName()), fieldName), MULTILINE);
    }

    private Pattern callMethodRegex(Class<?> origin, Class<?> targetClass, String methodName, Class<?> paramType) {
        return callMethodRegex(origin, targetClass, methodName, paramType.getName());
    }

    private Pattern callMethodRegex(Class<?> origin, Class<?> targetClass, String methodName) {
        return callMethodRegex(origin, targetClass, methodName, "");
    }

    private Pattern callMethodRegex(Class<?> origin, Class<?> targetClass, String methodName, String paramTypeName) {
        return Pattern.compile(String.format(".*%s.* method.*%s\\.%s\\(%s\\).*",
                quote(origin.getName()), quote(targetClass.getName()), methodName, quote(paramTypeName)));
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

        ClassWithField wrongAccess() {
            classAccessingField.classWithField = null;
            return classAccessingField.classWithField;
        }
    }

    private static class ClassWithMethod {
        void method(String param) {
        }
    }

    private static class ClassCallingMethod {
        ClassWithMethod classWithMethod;

        void call() {
            classWithMethod.method("param");
        }
    }

    private static class ClassCallingWrongMethod {
        ClassCallingMethod classCallingMethod;

        void callWrong() {
            classCallingMethod.call();
        }
    }
}
