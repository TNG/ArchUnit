package com.tngtech.archunit.lang.syntax.elements;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CanBeEvaluated;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
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
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class GivenMembersTest {

    @DataProvider
    public static Object[][] member_syntax_testcases() {
        return $$(
                $(members(), "members", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", FIELD_ANNOTATED_WITH_B_AND_C),
                                String.format("Member '%s' is annotated with @C", CONSTRUCTOR_ANNOTATED_WITH_B_AND_C),
                                String.format("Member '%s' is annotated with @C", METHOD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(noMembers(), "no members", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", FIELD_ANNOTATED_WITH_B_AND_C),
                                String.format("Member '%s' is annotated with @C", CONSTRUCTOR_ANNOTATED_WITH_B_AND_C),
                                String.format("Member '%s' is annotated with @C", METHOD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(fields(), "fields", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", FIELD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(noFields(), "no fields", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", FIELD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(codeUnits(), "code units", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", CONSTRUCTOR_ANNOTATED_WITH_B_AND_C),
                                String.format("Member '%s' is annotated with @C", METHOD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(noCodeUnits(), "no code units", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", CONSTRUCTOR_ANNOTATED_WITH_B_AND_C),
                                String.format("Member '%s' is annotated with @C", METHOD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(methods(), "methods", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", METHOD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(noMethods(), "no methods", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", METHOD_ANNOTATED_WITH_B_AND_C)
                        )),
                $(constructors(), "constructors", never(beAnnotatedWith(C.class)),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", CONSTRUCTOR_ANNOTATED_WITH_B_AND_C)
                        )),
                $(noConstructors(), "no constructors", beAnnotatedWith(C.class),
                        ImmutableList.of(
                                String.format("Member '%s' is annotated with @C", CONSTRUCTOR_ANNOTATED_WITH_B_AND_C)
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

        assertViolation(result);
        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedViolationDetails);
    }

    @Test
    public void complex_members_syntax() {
        EvaluationResult result = members()
                .that().haveName(FIELD_A)
                .or().arePublic()
                .and(areNoFieldsWithType(List.class))
                .should(beAnnotatedWith(B.class))
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        assertThat(result.getFailureReport().getDetails()).containsOnly(
                String.format("Member '%s' is not annotated with @B", FIELD_A),
                String.format("Member '%s' is not annotated with @B", CONSTRUCTOR_THREE_ARGS),
                String.format("Member '%s' is not annotated with @B", METHOD_C));
    }

    @DataProvider
    public static Object[][] restricted_property_rule_starts() {
        String classNameDot = ClassWithVariousMembers.class.getName() + ".";
        ImmutableList.Builder<Object[]> data = ImmutableList.<Object[]>builder().add(
                $(described(members().that().haveName(FIELD_A)), ImmutableSet.of(FIELD_A)),
                $(described(codeUnits().that().haveName(FIELD_A)), ImmutableSet.of()),
                $(described(fields().that().haveName(FIELD_A)), ImmutableSet.of(FIELD_A)),
                $(described(codeUnits().that().haveName("methodA")), ImmutableSet.of(METHOD_A)),
                $(described(methods().that().haveName("methodA")), ImmutableSet.of(METHOD_A)),
                $(described(codeUnits().that().haveName(CONSTRUCTOR_NAME)), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(described(constructors().that().haveName(CONSTRUCTOR_NAME)), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(described(members().that().doNotHaveName(FIELD_A)), union(
                        allFieldsExcept(FIELD_A),
                        ALL_CODE_UNIT_DESCRIPTIONS)),
                $(described(codeUnits().that().doNotHaveName(FIELD_A)), ALL_CODE_UNIT_DESCRIPTIONS),
                $(described(fields().that().doNotHaveName(FIELD_A)), allFieldsExcept(FIELD_A)),
                $(described(codeUnits().that().doNotHaveName("methodA")), union(
                        allMethodsExcept(METHOD_A),
                        ALL_CONSTRUCTOR_DESCRIPTIONS)),
                $(described(methods().that().doNotHaveName("methodA")), allMethodsExcept(METHOD_A)),
                $(described(codeUnits().that().doNotHaveName(CONSTRUCTOR_NAME)), ALL_METHOD_DESCRIPTIONS),
                $(described(constructors().that().doNotHaveName(CONSTRUCTOR_NAME)), ImmutableSet.of()),

                $(described(members().that().haveNameMatching("f.*A")), ImmutableSet.of(FIELD_A)),
                $(described(codeUnits().that().haveNameMatching("f.*A")), ImmutableSet.of()),
                $(described(fields().that().haveNameMatching("f.*A")), ImmutableSet.of(FIELD_A)),
                $(described(codeUnits().that().haveNameMatching("m.*A")), ImmutableSet.of(METHOD_A)),
                $(described(methods().that().haveNameMatching("m.*A")), ImmutableSet.of(METHOD_A)),
                $(described(codeUnits().that().haveNameMatching(".*init.*")), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(described(constructors().that().haveNameMatching(".*init.*")), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(described(members().that().haveNameNotMatching("f.*A")), union(
                        allFieldsExcept(FIELD_A),
                        ALL_CODE_UNIT_DESCRIPTIONS)),
                $(described(codeUnits().that().haveNameNotMatching("f.*A")), ALL_CODE_UNIT_DESCRIPTIONS),
                $(described(fields().that().haveNameNotMatching("f.*A")), allFieldsExcept(FIELD_A)),
                $(described(codeUnits().that().haveNameNotMatching("m.*A")), union(
                        allMethodsExcept(METHOD_A),
                        ALL_CONSTRUCTOR_DESCRIPTIONS)),
                $(described(methods().that().haveNameNotMatching("m.*A")), allMethodsExcept(METHOD_A)),
                $(described(codeUnits().that().haveNameNotMatching(".*init.*")), ALL_METHOD_DESCRIPTIONS),
                $(described(constructors().that().haveNameNotMatching(".*init.*")), emptySet()),

                $(described(members().that().haveFullName(classNameDot + FIELD_A)), ImmutableSet.of(FIELD_A)),
                $(described(codeUnits().that().haveFullName(classNameDot + METHOD_A)), ImmutableSet.of(METHOD_A)),
                $(described(methods().that().haveFullName(classNameDot + METHOD_A)), ImmutableSet.of(METHOD_A)),
                $(described(constructors().that().haveFullName(classNameDot + CONSTRUCTOR_ONE_ARG)), ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),
                $(described(fields().that().haveFullName(classNameDot + FIELD_A)), ImmutableSet.of(FIELD_A)),
                $(described(members().that().doNotHaveFullName(classNameDot + FIELD_A)), union(allFieldsExcept(FIELD_A), ALL_CODE_UNIT_DESCRIPTIONS)),
                $(described(codeUnits().that().doNotHaveFullName(classNameDot + METHOD_A)), allCodeUnitsExcept(METHOD_A)),
                $(described(methods().that().doNotHaveFullName(classNameDot + METHOD_A)), allMethodsExcept(METHOD_A)),
                $(described(constructors().that().doNotHaveFullName(classNameDot + CONSTRUCTOR_ONE_ARG)), allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(described(fields().that().doNotHaveFullName(classNameDot + FIELD_A)), allFieldsExcept(FIELD_A)),

                $(described(members().that().haveFullNameMatching(quote(classNameDot) + ".*A.*")), ImmutableSet.of(FIELD_A, METHOD_A)),
                $(described(codeUnits().that().haveFullNameMatching(quote(classNameDot) + ".*A.*")), ImmutableSet.of(METHOD_A)),
                $(described(methods().that().haveFullNameMatching(quote(classNameDot) + ".*A.*")), ImmutableSet.of(METHOD_A)),
                $(described(constructors().that().haveFullNameMatching(quote(classNameDot) + ".*init.*String\\)")),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),
                $(described(fields().that().haveFullNameMatching(quote(classNameDot) + ".*A.*")), ImmutableSet.of(FIELD_A)),
                $(described(members().that().haveFullNameNotMatching(quote(classNameDot) + "f.*A.*")), union(
                        allFieldsExcept(FIELD_A),
                        ALL_CODE_UNIT_DESCRIPTIONS)),
                $(described(codeUnits().that().haveFullNameNotMatching(quote(classNameDot) + "f.*A.*")), ALL_CODE_UNIT_DESCRIPTIONS),
                $(described(methods().that().haveFullNameNotMatching(quote(classNameDot) + ".*A.*")), allMethodsExcept(METHOD_A)),
                $(described(constructors().that().haveFullNameNotMatching(quote(classNameDot) + ".*init.*String\\)")),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(described(fields().that().haveFullNameNotMatching(quote(classNameDot) + ".*A.*")), allFieldsExcept(FIELD_A)),

                $(described(members().that().arePublic()), ImmutableSet.of(
                        FIELD_PUBLIC, METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(described(fields().that().arePublic()), ImmutableSet.of(FIELD_C)),
                $(described(codeUnits().that().arePublic()), ImmutableSet.of(METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(described(methods().that().arePublic()), ImmutableSet.of(METHOD_PUBLIC)),
                $(described(constructors().that().arePublic()), ImmutableSet.of(CONSTRUCTOR_PUBLIC)),
                $(described(members().that().areNotPublic()),
                        allMembersExcept(FIELD_PUBLIC, METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(described(fields().that().areNotPublic()), allFieldsExcept(FIELD_C)),
                $(described(codeUnits().that().areNotPublic()),
                        allCodeUnitsExcept(METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(described(methods().that().areNotPublic()), allMethodsExcept(METHOD_PUBLIC)),
                $(described(constructors().that().areNotPublic()), allConstructorsExcept(CONSTRUCTOR_PUBLIC)),

                $(described(members().that().areProtected()), ImmutableSet.of(
                        FIELD_PROTECTED, METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(described(fields().that().areProtected()), ImmutableSet.of(FIELD_PROTECTED)),
                $(described(codeUnits().that().areProtected()), ImmutableSet.of(
                        METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(described(methods().that().areProtected()), ImmutableSet.of(METHOD_PROTECTED)),
                $(described(constructors().that().areProtected()), ImmutableSet.of(CONSTRUCTOR_PROTECTED)),
                $(described(members().that().areNotProtected()),
                        allMembersExcept(FIELD_PROTECTED, METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(described(fields().that().areNotProtected()), allFieldsExcept(FIELD_PROTECTED)),
                $(described(codeUnits().that().areNotProtected()),
                        allCodeUnitsExcept(METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(described(methods().that().areNotProtected()), allMethodsExcept(METHOD_PROTECTED)),
                $(described(constructors().that().areNotProtected()), allConstructorsExcept(CONSTRUCTOR_PROTECTED)),

                $(described(members().that().arePackagePrivate()), ImmutableSet.of(
                        FIELD_PACKAGE_PRIVATE, METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(described(fields().that().arePackagePrivate()), ImmutableSet.of(FIELD_PACKAGE_PRIVATE)),
                $(described(codeUnits().that().arePackagePrivate()), ImmutableSet.of(
                        METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(described(methods().that().arePackagePrivate()), ImmutableSet.of(METHOD_PACKAGE_PRIVATE)),
                $(described(constructors().that().arePackagePrivate()), ImmutableSet.of(CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(described(members().that().areNotPackagePrivate()),
                        allMembersExcept(FIELD_PACKAGE_PRIVATE, METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(described(fields().that().areNotPackagePrivate()), allFieldsExcept(FIELD_PACKAGE_PRIVATE)),
                $(described(codeUnits().that().areNotPackagePrivate()),
                        allCodeUnitsExcept(METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(described(methods().that().areNotPackagePrivate()), allMethodsExcept(METHOD_PACKAGE_PRIVATE)),
                $(described(constructors().that().areNotPackagePrivate()), allConstructorsExcept(CONSTRUCTOR_PACKAGE_PRIVATE)),

                $(described(members().that().arePrivate()), ImmutableSet.of(
                        FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(fields().that().arePrivate()), ImmutableSet.of(FIELD_PRIVATE)),
                $(described(codeUnits().that().arePrivate()), ImmutableSet.of(
                        METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(methods().that().arePrivate()), ImmutableSet.of(METHOD_PRIVATE)),
                $(described(constructors().that().arePrivate()), ImmutableSet.of(CONSTRUCTOR_PRIVATE)),
                $(described(members().that().areNotPrivate()),
                        allMembersExcept(FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(fields().that().areNotPrivate()), allFieldsExcept(FIELD_PRIVATE)),
                $(described(codeUnits().that().areNotPrivate()),
                        allCodeUnitsExcept(METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(methods().that().areNotPrivate()), allMethodsExcept(METHOD_PRIVATE)),
                $(described(constructors().that().areNotPrivate()), allConstructorsExcept(CONSTRUCTOR_PRIVATE)),

                $(described(members().that().haveModifier(PRIVATE)), ImmutableSet.of(
                        FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(fields().that().haveModifier(PRIVATE)), ImmutableSet.of(FIELD_PRIVATE)),
                $(described(codeUnits().that().haveModifier(PRIVATE)), ImmutableSet.of(
                        METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(methods().that().haveModifier(PRIVATE)), ImmutableSet.of(METHOD_PRIVATE)),
                $(described(constructors().that().haveModifier(PRIVATE)), ImmutableSet.of(CONSTRUCTOR_PRIVATE)),
                $(described(members().that().doNotHaveModifier(PRIVATE)),
                        allMembersExcept(FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(fields().that().doNotHaveModifier(PRIVATE)), allFieldsExcept(FIELD_PRIVATE)),
                $(described(codeUnits().that().doNotHaveModifier(PRIVATE)),
                        allCodeUnitsExcept(METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(described(methods().that().doNotHaveModifier(PRIVATE)), allMethodsExcept(METHOD_PRIVATE)),
                $(described(constructors().that().doNotHaveModifier(PRIVATE)), allConstructorsExcept(CONSTRUCTOR_PRIVATE)));

        data.add(annotatedWithDataPoints(
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areAnnotatedWith(A.class);
                    }
                },
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areNotAnnotatedWith(A.class);
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areAnnotatedWith(A.class.getName());
                    }
                },
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areNotAnnotatedWith(A.class.getName());
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(A.class)));
                    }
                },
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areNotAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(A.class)));
                    }
                }));

        data.add(annotatedWithDataPoints(
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areMetaAnnotatedWith(MetaAnnotation.class);
                    }
                },
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areNotMetaAnnotatedWith(MetaAnnotation.class);
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areMetaAnnotatedWith(MetaAnnotation.class.getName());
                    }
                },
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areNotMetaAnnotatedWith(MetaAnnotation.class.getName());
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(MetaAnnotation.class)));
                    }
                },
                new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                    @Override
                    public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                        return membersThat.areNotMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(MetaAnnotation.class)));
                    }
                }));
        return data.build().toArray(new Object[0][]);
    }

    private static Object[][] annotatedWithDataPoints(
            Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>> makeAnnotatedWithMatchingA,
            Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>> makeNotAnnotatedWithMatchingA) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersThat, GivenMembersConjunction<?>> areAnnotatedWithA = (Function) makeAnnotatedWithMatchingA;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersThat, GivenMembersConjunction<?>> areNotAnnotatedWithA = (Function) makeNotAnnotatedWithMatchingA;
        return $$(
                $(described(areAnnotatedWithA.apply(members().that())), ImmutableSet.of(
                        FIELD_ANNOTATED_WITH_A, METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(described(areAnnotatedWithA.apply(fields().that())), ImmutableSet.of(FIELD_ANNOTATED_WITH_A)),
                $(described(areAnnotatedWithA.apply(codeUnits().that())), ImmutableSet.of(
                        METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(described(areAnnotatedWithA.apply(methods().that())), ImmutableSet.of(METHOD_ANNOTATED_WITH_A)),
                $(described(areAnnotatedWithA.apply(constructors().that())), ImmutableSet.of(CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(described(areNotAnnotatedWithA.apply(members().that())),
                        allMembersExcept(FIELD_ANNOTATED_WITH_A, METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(described(areNotAnnotatedWithA.apply(fields().that())), allFieldsExcept(FIELD_ANNOTATED_WITH_A)),
                $(described(areNotAnnotatedWithA.apply(codeUnits().that())),
                        allCodeUnitsExcept(METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(described(areNotAnnotatedWithA.apply(methods().that())), allMethodsExcept(METHOD_ANNOTATED_WITH_A)),
                $(described(areNotAnnotatedWithA.apply(constructors().that())), allConstructorsExcept(CONSTRUCTOR_ANNOTATED_WITH_A)));
    }

    @Test
    @UseDataProvider("restricted_property_rule_starts")
    public void property_predicates(DescribedRuleStart conjunction, Set<String> expectedMessages) {
        EvaluationResult result = conjunction.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class, A.class, B.class, C.class, MetaAnnotation.class));

        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedMessages);
    }

    @DataProvider
    public static Object[][] restricted_declaration_rule_starts() {
        return ImmutableList.<Object[]>builder().add(
                declaredInDataPoints(
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areDeclaredIn(ClassWithVariousMembers.class);
                            }
                        },
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areNotDeclaredIn(ClassWithVariousMembers.class);
                            }
                        }
                )).add(
                declaredInDataPoints(
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areDeclaredIn(ClassWithVariousMembers.class.getName());
                            }
                        },
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areNotDeclaredIn(ClassWithVariousMembers.class.getName());
                            }
                        }
                )).add(
                declaredInDataPoints(
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areDeclaredInClassesThat(equivalentTo(ClassWithVariousMembers.class));
                            }
                        },
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areDeclaredInClassesThat(not(equivalentTo(ClassWithVariousMembers.class)));
                            }
                        }
                )).add(
                declaredInDataPoints(
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areDeclaredInClassesThat().areAssignableTo(ClassWithVariousMembers.class);
                            }
                        },
                        new Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>>() {
                            @Override
                            public GivenMembersConjunction<?> apply(MembersThat<GivenMembersConjunction<?>> membersThat) {
                                return membersThat.areDeclaredInClassesThat().areNotAssignableTo(ClassWithVariousMembers.class);
                            }
                        }
                )).build().toArray(new Object[0][]);
    }

    private static Object[][] declaredInDataPoints(
            Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>> makeDeclaredInClassWithVariousMembers,
            Function<MembersThat<GivenMembersConjunction<?>>, GivenMembersConjunction<?>> makeNotDeclaredInClassWithVariousMembers) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersThat, GivenMembersConjunction<?>> areDeclaredInClass = (Function) makeDeclaredInClassWithVariousMembers;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersThat, GivenMembersConjunction<?>> areNotDeclaredInClass = (Function) makeNotDeclaredInClassWithVariousMembers;
        return $$(
                $(described(areDeclaredInClass.apply(members().that())), ALL_MEMBER_DESCRIPTIONS),
                $(described(areDeclaredInClass.apply(fields().that())), ALL_FIELD_DESCRIPTIONS),
                $(described(areDeclaredInClass.apply(codeUnits().that())), ALL_CODE_UNIT_DESCRIPTIONS),
                $(described(areDeclaredInClass.apply(methods().that())), ALL_METHOD_DESCRIPTIONS),
                $(described(areDeclaredInClass.apply(constructors().that())), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(described(areNotDeclaredInClass.apply(members().that())), ALL_OTHER_MEMBER_DESCRIPTIONS),
                $(described(areNotDeclaredInClass.apply(fields().that())), ALL_OTHER_FIELD_DESCRIPTIONS),
                $(described(areNotDeclaredInClass.apply(codeUnits().that())), ALL_OTHER_CODE_UNIT_DESCRIPTIONS),
                $(described(areNotDeclaredInClass.apply(methods().that())), ALL_OTHER_METHOD_DESCRIPTIONS),
                $(described(areNotDeclaredInClass.apply(constructors().that())), ALL_OTHER_CONSTRUCTOR_DESCRIPTIONS));
    }

    @Test
    @UseDataProvider("restricted_declaration_rule_starts")
    public void declaration_predicates(DescribedRuleStart conjunction, Set<String> expectedMessages) {
        EvaluationResult result = conjunction.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class, OtherClassWithMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedMessages);
    }

    static void assertViolation(EvaluationResult result) {
        assertThat(result.hasViolation()).as("result has violation").isTrue();
    }

    static ArchCondition<JavaMember> everythingViolationPrintMemberName() {
        return new ArchCondition<JavaMember>("condition text") {
            @Override
            public void check(JavaMember item, ConditionEvents events) {
                events.add(SimpleConditionEvent.violated(item, formatMember(item)));
            }
        };
    }

    @SafeVarargs
    private static <T> Set<T> union(Set<T>... sets) {
        FluentIterable<T> result = FluentIterable.of();
        for (Set<T> set : sets) {
            result = result.append(set);
        }
        return result.toSet();
    }

    static DescribedPredicate<JavaMember> areNoFieldsWithType(final Class<?> type) {
        return new DescribedPredicate<JavaMember>("are no fields with type " + type.getSimpleName()) {
            @Override
            public boolean apply(JavaMember member) {
                return !(member instanceof JavaField) || !((JavaField) member).getRawType().isEquivalentTo(type);
            }
        };
    }

    static ArchCondition<JavaMember> beAnnotatedWith(final Class<? extends Annotation> annotationType) {
        return new ArchCondition<JavaMember>("be annotated with @%s", annotationType.getSimpleName()) {
            @Override
            public void check(JavaMember member, ConditionEvents events) {
                boolean satisfied = member.isAnnotatedWith(annotationType);
                String message = String.format("Member '%s' %s @%s",
                        formatMember(member), satisfied ? "is annotated with" : "is not annotated with",
                        annotationType.getSimpleName());
                events.add(new SimpleConditionEvent(member, satisfied, message));
            }
        };
    }

    private static String formatMember(JavaMember member) {
        return member.getFullName().replaceAll("^[^(]*\\.", "");
    }

    static DescribedRuleStart described(GivenMembersConjunction<?> conjunction) {
        return new DescribedRuleStart(conjunction);
    }

    static class DescribedRuleStart {
        private final GivenMembersConjunction<?> conjunction;

        DescribedRuleStart(GivenMembersConjunction<?> conjunction) {
            this.conjunction = conjunction;
        }

        CanBeEvaluated should(ArchCondition<JavaMember> condition) {
            return conjunction.should(condition);
        }

        @Override
        public String toString() {
            return conjunction.should(new ArchCondition<Object>("removeMe") {
                @Override
                public void check(Object item, ConditionEvents events) {
                }
            }).getDescription().replace("should removeMe", "");
        }
    }

    static Set<String> allMembersExcept(String... memberDescriptions) {
        return Sets.difference(
                ALL_MEMBER_DESCRIPTIONS,
                ImmutableSet.copyOf(memberDescriptions));
    }

    static Set<String> allCodeUnitsExcept(String... codeUnitDescriptions) {
        return Sets.difference(
                ALL_CODE_UNIT_DESCRIPTIONS,
                ImmutableSet.copyOf(codeUnitDescriptions));
    }

    static Set<String> allFieldsExcept(String fieldDescription) {
        return Sets.difference(ALL_FIELD_DESCRIPTIONS, singleton(fieldDescription));
    }

    static Set<String> allMethodsExcept(String methodDescription) {
        return Sets.difference(ALL_METHOD_DESCRIPTIONS, singleton(methodDescription));
    }

    static Set<String> allConstructorsExcept(String constructorDescription) {
        return Sets.difference(ALL_CONSTRUCTOR_DESCRIPTIONS, singleton(constructorDescription));
    }

    static final String FIELD_A = "fieldA";
    static final String FIELD_B = "fieldB";
    static final String FIELD_C = "fieldC";
    static final String FIELD_D = "fieldD";
    static final String METHOD_A = "methodA()";
    static final String METHOD_B = "methodB()";
    static final String METHOD_C = "methodC()";
    static final String METHOD_D = "methodD()";
    static final String CONSTRUCTOR_ONE_ARG = "<init>(java.lang.String)";
    static final String CONSTRUCTOR_TWO_ARGS = "<init>(java.lang.String, java.lang.Object)";
    static final String CONSTRUCTOR_THREE_ARGS = "<init>(java.lang.String, java.lang.Object, java.util.List)";
    static final String CONSTRUCTOR_FOUR_ARGS = "<init>(java.lang.String, java.lang.Object, java.util.List, int)";

    static final String FIELD_PUBLIC = FIELD_C;
    static final String METHOD_PUBLIC = METHOD_C;
    static final String CONSTRUCTOR_PUBLIC = CONSTRUCTOR_THREE_ARGS;

    static final String FIELD_PROTECTED = FIELD_B;
    static final String METHOD_PROTECTED = METHOD_B;
    static final String CONSTRUCTOR_PROTECTED = CONSTRUCTOR_TWO_ARGS;

    static final String FIELD_PACKAGE_PRIVATE = FIELD_D;
    static final String METHOD_PACKAGE_PRIVATE = METHOD_D;
    static final String CONSTRUCTOR_PACKAGE_PRIVATE = CONSTRUCTOR_FOUR_ARGS;

    static final String FIELD_PRIVATE = FIELD_A;
    static final String METHOD_PRIVATE = METHOD_A;
    static final String CONSTRUCTOR_PRIVATE = CONSTRUCTOR_ONE_ARG;

    static final String FIELD_ANNOTATED_WITH_A = FIELD_A;
    static final String METHOD_ANNOTATED_WITH_A = METHOD_A;
    static final String CONSTRUCTOR_ANNOTATED_WITH_A = CONSTRUCTOR_ONE_ARG;

    static final String FIELD_ANNOTATED_WITH_B_AND_C = FIELD_B;
    static final String METHOD_ANNOTATED_WITH_B_AND_C = METHOD_B;
    static final String CONSTRUCTOR_ANNOTATED_WITH_B_AND_C = CONSTRUCTOR_TWO_ARGS;

    static final Set<String> ALL_FIELD_DESCRIPTIONS = ImmutableSet.of(
            FIELD_A, FIELD_B, FIELD_C, FIELD_D);
    static final Set<String> ALL_METHOD_DESCRIPTIONS = ImmutableSet.of(
            METHOD_A, METHOD_B, METHOD_C, METHOD_D);
    static final Set<String> ALL_CONSTRUCTOR_DESCRIPTIONS = ImmutableSet.of(
            CONSTRUCTOR_ONE_ARG,
            CONSTRUCTOR_TWO_ARGS,
            CONSTRUCTOR_THREE_ARGS,
            CONSTRUCTOR_FOUR_ARGS);
    static final Set<String> ALL_CODE_UNIT_DESCRIPTIONS =
            union(ALL_METHOD_DESCRIPTIONS, ALL_CONSTRUCTOR_DESCRIPTIONS);
    static final Set<String> ALL_MEMBER_DESCRIPTIONS =
            union(ALL_CODE_UNIT_DESCRIPTIONS, ALL_FIELD_DESCRIPTIONS);

    @SuppressWarnings({"unused"})
    static class ClassWithVariousMembers {
        @A
        private String fieldA;
        @B
        @C
        protected Object fieldB;
        @C
        public List<?> fieldC;
        Map<?, ?> fieldD;

        @A
        private ClassWithVariousMembers(String fieldA) {
            this.fieldA = fieldA;
        }

        @B
        @C
        protected ClassWithVariousMembers(String fieldA, Object fieldB) {
        }

        @C
        public ClassWithVariousMembers(String fieldA, Object fieldB, List<?> fieldC) {
        }

        ClassWithVariousMembers(String fieldA, Object fieldB, List<?> fieldC, int i) {
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

        int methodD() {
            return 0;
        }
    }

    static Set<String> ALL_OTHER_FIELD_DESCRIPTIONS = ImmutableSet.of("otherField");
    static Set<String> ALL_OTHER_METHOD_DESCRIPTIONS = ImmutableSet.of("otherMethod()");
    static Set<String> ALL_OTHER_CONSTRUCTOR_DESCRIPTIONS = ImmutableSet.of("<init>(java.io.Serializable)");
    static Set<String> ALL_OTHER_CODE_UNIT_DESCRIPTIONS =
            union(ALL_OTHER_METHOD_DESCRIPTIONS, ALL_OTHER_CONSTRUCTOR_DESCRIPTIONS);
    static Set<String> ALL_OTHER_MEMBER_DESCRIPTIONS =
            union(ALL_OTHER_CODE_UNIT_DESCRIPTIONS, ALL_OTHER_FIELD_DESCRIPTIONS);

    @SuppressWarnings("unused")
    static class OtherClassWithMembers {
        String otherField;

        public OtherClassWithMembers(Serializable other) {
        }

        void otherMethod() {
        }
    }

    @MetaAnnotation
    @interface A {
    }

    @interface B {
    }

    @interface C {
    }

    @interface MetaAnnotation {
    }
}
