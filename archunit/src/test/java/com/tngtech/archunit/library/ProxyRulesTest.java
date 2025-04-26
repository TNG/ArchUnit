package com.tngtech.archunit.library;

import java.util.stream.Stream;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.ProxyRules.directly_call_other_methods_declared_in_the_same_class_that;
import static com.tngtech.archunit.library.ProxyRules.directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with;
import static com.tngtech.archunit.library.ProxyRules.no_classes_should_directly_call_other_methods_declared_in_the_same_class_that;
import static com.tngtech.archunit.library.ProxyRules.no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static java.util.regex.Pattern.quote;

public class ProxyRulesTest {

    static Stream<Arguments> call_own_method_with_specific_annotation_rules() {
        return Stream.of(
                $(no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(ProxyAnnotation.class), String.format(
                        "no classes should directly call other methods declared in the same class that are annotated with @%s, because it bypasses the proxy mechanism",
                        ProxyAnnotation.class.getSimpleName())),
                $(noClasses().should(directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(ProxyAnnotation.class)), String.format(
                        "no classes should directly call other methods declared in the same class that are annotated with @%s",
                        ProxyAnnotation.class.getSimpleName())),
                $(no_classes_should_directly_call_other_methods_declared_in_the_same_class_that(have(name("selfProxied"))),
                        "no classes should directly call other methods declared in the same class that have name 'selfProxied', because it bypasses the proxy mechanism"),
                $(noClasses().should(directly_call_other_methods_declared_in_the_same_class_that(have(name("selfProxied")))),
                        "no classes should directly call other methods declared in the same class that have name 'selfProxied'")
        );
    }

    @ParameterizedTest
    @MethodSource("call_own_method_with_specific_annotation_rules")
    void detects_direct_self_call_to_method_annotated_with_specific_annotation(ArchRule rule, String expectedDescription) {
        class OtherClass {
            @ProxyAnnotation
            void otherProxied() {
            }
        }
        @SuppressWarnings("unused")
        class SomeClass {
            void evil() {
                selfProxied();
            }

            void okay() {
                new OtherClass().otherProxied();
                nonProxied();
            }

            void nonProxied() {
            }

            @ProxyAnnotation
            void selfProxied() {
            }
        }

        assertThatRule(rule)
                .hasDescriptionContaining(expectedDescription)
                .checking(new ClassFileImporter().importClasses(SomeClass.class, OtherClass.class))
                .hasOnlyOneViolationMatching(String.format(".*%s.* calls method .*%s.*",
                        quoteMethod(SomeClass.class, "evil"), quoteMethod(SomeClass.class, "selfProxied")));
    }

    @ParameterizedTest
    @MethodSource("call_own_method_with_specific_annotation_rules")
    void ignores_synthetic_bridge_method_calling_other_method_declared_in_the_same_class(ArchRule rule, String expectedDescription) {
        abstract class GenericBaseClass<T> {
            @SuppressWarnings("unused")
            abstract void selfProxied(T value);
        }

        class SpecificChild extends GenericBaseClass<String> {
            @Override
            @ProxyAnnotation
            void selfProxied(String value) {
            }

            // The compiler generates a synthetic bridge method `void selfProxied(java.lang.Object)`
            // that delegates to `void selfProxied(String value)`.
        }

        assertThatRule(rule)
                .hasDescriptionContaining(expectedDescription)
                .checking(new ClassFileImporter().importClasses(GenericBaseClass.class, SpecificChild.class))
                .hasNoViolation();
    }

    private String quoteMethod(Class<?> owner, String methodName) {
        return quote(owner.getSimpleName() + "." + methodName + "()");
    }

    private @interface ProxyAnnotation {
    }
}
