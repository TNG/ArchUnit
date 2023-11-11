package com.tngtech.archunit.lang.syntax.elements;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.areNoFieldsWithType;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.lang.syntax.elements.MembersShouldTest.parseMembers;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class FieldsShouldTest {

    @Test
    public void complex_field_syntax() {
        EvaluationResult result = fields()
                .that(areNoFieldsWithType(List.class))
                .and().haveNameMatching(".*field(A|D).*")
                .should().beAnnotatedWith(A.class)
                .andShould().notBePublic()
                .orShould().haveRawType(Map.class)
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        String failure = getOnlyElement(result.getFailureReport().getDetails());
        assertThat(failure)
                .containsPattern(String.format("%s.* does not have raw type %s", FIELD_A, Map.class.getName()))
                .containsPattern(String.format("%s.* is not annotated with @%s", FIELD_A, A.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] restricted_property_rule_ends() {
        return $$(
                $(fields().should().haveRawType(String.class), ImmutableList.of(FIELD_B, FIELD_C, FIELD_D)),
                $(fields().should().haveRawType(String.class.getName()), ImmutableList.of(FIELD_B, FIELD_C, FIELD_D)),
                $(fields().should().haveRawType(equivalentTo(String.class).as(String.class.getName())), ImmutableList.of(FIELD_B, FIELD_C, FIELD_D)),
                $(fields().should().notHaveRawType(String.class), ImmutableList.of(FIELD_A)),
                $(fields().should().notHaveRawType(String.class.getName()), ImmutableList.of(FIELD_A)),
                $(fields().should().notHaveRawType(equivalentTo(String.class).as(String.class.getName())), ImmutableList.of(FIELD_A)),
                $(fields().should().beFinal(), ImmutableList.of(FIELD_C, FIELD_D)),
                $(fields().should().notBeFinal(), ImmutableList.of(FIELD_A, FIELD_B)),
                $(fields().should().beStatic(), ImmutableList.of(FIELD_A, FIELD_C)),
                $(fields().should().notBeStatic(), ImmutableList.of(FIELD_B, FIELD_D))
        );
    }

    @Test
    @UseDataProvider("restricted_property_rule_ends")
    public void property_predicates(ArchRule rule, Collection<String> expectedViolatingFields) {
        EvaluationResult result = rule
                .evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualFields = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualFields).hasSameElementsAs(expectedViolatingFields);
    }

    @DataProvider
    public static Object[][] data_be_accessed_by_methods() {
        return testForEach(
                fields().should().notBeAccessedByMethodsThat(have(name("toFind"))),
                noFields().should().beAccessedByMethodsThat(have(name("toFind")))
        );
    }

    @Test
    @UseDataProvider
    public void test_be_accessed_by_methods(ArchRule ruleCheckingAccessToMethod) {
        class ClassWithAccessedField {
            String field;
        }
        @SuppressWarnings("unused")
        class AccessFromMethod {
            void toFind(ClassWithAccessedField target) {
                target.field = "changed";
            }

            void toIgnore(ClassWithAccessedField target) {
                target.field = "changed";
            }
        }

        assertThatRule(ruleCheckingAccessToMethod)
                .checking(new ClassFileImporter().importClasses(ClassWithAccessedField.class, AccessFromMethod.class))
                .hasOnlyOneViolationMatching(".*" + quote(AccessFromMethod.class.getName()) + ".*toFind.*")
                .hasNoViolationMatching(".*toIgnore.*");
    }

    private static final String FIELD_A = "fieldA";
    private static final String FIELD_B = "fieldB";
    private static final String FIELD_C = "fieldC";
    private static final String FIELD_D = "fieldD";

    @SuppressWarnings({"unused"})
    private static class ClassWithVariousMembers {
        private final String fieldA = "A";
        @A
        protected static final Object fieldB = 'B';
        public List<?> fieldC;
        static Map<?, ?> fieldD;
    }

    private @interface A {
    }
}
