package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class GivenMembersTest {

    @DataProvider
    public static Object[][] member_syntax_testcases() {
        return $$(
                $(members(), "members", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                "Name 'fieldB' is annotated with @C",
                                "Name '<init>' is annotated with @C",
                                "Name 'methodB' is annotated with @C"
                        )),
                $(noMembers(), "no members", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                "Name 'fieldB' is annotated with @C",
                                "Name '<init>' is annotated with @C",
                                "Name 'methodB' is annotated with @C"
                        )),
                $(fields(), "fields", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                "Name 'fieldB' is annotated with @C"
                        )),
                $(noFields(), "no fields", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                "Name 'fieldB' is annotated with @C"
                        )),
                $(codeUnits(), "code units", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                "Name '<init>' is annotated with @C",
                                "Name 'methodB' is annotated with @C"
                        )),
                $(noCodeUnits(), "no code units", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                "Name '<init>' is annotated with @C",
                                "Name 'methodB' is annotated with @C"
                        )),
                $(methods(), "methods", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                "Name 'methodB' is annotated with @C"
                        )),
                $(noMethods(), "no methods", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                "Name 'methodB' is annotated with @C"
                        )),
                $(constructors(), "constructors", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                "Name '<init>' is annotated with @C"
                        )),
                $(noConstructors(), "no constructors", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                "Name '<init>' is annotated with @C"
                        ))
        );
    }

    @Test
    @UseDataProvider("member_syntax_testcases")
    public void test_members(GivenMembers<JavaMember> members, String ruleStart, ArchCondition<JavaMember> condition,
            List<String> expectedViolationDetails) {
        ArchRule rule = members
                .that(have(modifier(PRIVATE)))
                .or(have(modifier(PROTECTED)))
                .and(are(annotatedWith(B.class)))
                .should(condition);

        assertThat(rule.getDescription()).as("rule description")
                .isEqualTo(ruleStart + " that have modifier PRIVATE or have modifier PROTECTED and are annotated with @B "
                        + "should " + condition.getDescription());

        EvaluationResult result = rule.evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.hasViolation()).as("result has violation").isTrue();
        assertThat(result.getFailureReport().getDetails())
                .containsOnlyElementsOf(expectedViolationDetails);
    }

    private static ArchCondition<JavaMember> beAnnotatedWith(final Class<? extends Annotation> annotationType) {
        return new ArchCondition<JavaMember>("be annotated with @%s", annotationType.getSimpleName()) {
            @Override
            public void check(JavaMember member, ConditionEvents events) {
                boolean satisfied = member.isAnnotatedWith(annotationType);
                String message = String.format("Name '%s' %s @%s",
                        member.getName(), satisfied ? "is annotated with" : "is not annotated with", annotationType.getSimpleName());
                events.add(new SimpleConditionEvent(member, satisfied, message));
            }
        };
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    private static class ClassWithVariousMembers {
        @A
        private String fieldA;
        @B
        @C
        protected Object fieldB;
        @C
        public List<?> fieldC;

        @A
        private ClassWithVariousMembers(String fieldA) {
            this.fieldA = fieldA;
        }

        @B
        @C
        protected ClassWithVariousMembers(String fieldA, Object fieldB) {
            this.fieldA = fieldA;
            this.fieldB = fieldB;
        }

        @C
        public ClassWithVariousMembers(String fieldA, Object fieldB, List<?> fieldC) {
            this.fieldA = fieldA;
            this.fieldB = fieldB;
            this.fieldC = fieldC;
        }

        @A
        private String methodA() {
            return null;
        }

        @B
        @C
        protected Object methodB() {
            return null;
        }

        @C
        public List<?> methodC() {
            return null;
        }
    }

    private @interface A {
    }

    private @interface B {
    }

    private @interface C {
    }
}
