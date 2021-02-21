package com.tngtech.archunit.library;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.ProxyRules.directly_call_other_methods_declared_in_the_same_class_that;
import static com.tngtech.archunit.library.ProxyRules.directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with;
import static com.tngtech.archunit.library.ProxyRules.no_classes_should_directly_call_other_methods_declared_in_the_same_class_that;
import static com.tngtech.archunit.library.ProxyRules.no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.regex.Pattern.quote;

@RunWith(DataProviderRunner.class)
public class ProxyRulesTest {

    @DataProvider
    public static Object[][] call_own_method_with_specific_annotation_rules() {
        return $$(
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

    @Test
    @UseDataProvider("call_own_method_with_specific_annotation_rules")
    public void detects_direct_self_call_to_method_annotated_with_specific_annotation(ArchRule rule, String expectedDescription) {
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

    private String quoteMethod(Class<?> owner, String methodName) {
        return quote(owner.getSimpleName() + "." + methodName + "()");
    }

    private @interface ProxyAnnotation {
    }
}
