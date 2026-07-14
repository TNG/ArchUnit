package com.tngtech.archunit.lang.syntax.elements;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.A;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.B;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.C;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ClassWithVariousMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.MetaAnnotation;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.OtherClassWithMembers;
import com.tngtech.archunit.lang.syntax.elements.testclasses.SimpleFieldAndMethod;
import com.tngtech.archunit.testutil.ArchConfigurationExtension;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.locationPattern;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.singleLineFailureReportOf;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_CODE_UNIT_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_CONSTRUCTOR_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_FIELD_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_MEMBER_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_METHOD_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_OTHER_CODE_UNIT_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_OTHER_CONSTRUCTOR_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_OTHER_FIELD_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_OTHER_MEMBER_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_OTHER_METHOD_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.CONSTRUCTOR_ANNOTATED_WITH_A;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.CONSTRUCTOR_ONE_ARG;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.CONSTRUCTOR_PACKAGE_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.CONSTRUCTOR_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.CONSTRUCTOR_PROTECTED;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.CONSTRUCTOR_PUBLIC;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_A;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_ANNOTATED_WITH_A;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_B;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_C;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_D;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PACKAGE_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PROTECTED;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PUBLIC;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_A;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_ANNOTATED_WITH_A;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_B;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_C;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_D;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_PACKAGE_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_PROTECTED;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_PUBLIC;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.allCodeUnitsExcept;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.allConstructorsExcept;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.allFieldsExcept;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.allMembersExcept;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.allMethodsExcept;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.areNoFieldsWithType;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.util.Collections.emptySet;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class MembersShouldTest {

    @RegisterExtension
    ArchConfigurationExtension archConfiguration = new ArchConfigurationExtension();

    @Test
    public void complex_members_syntax() {
        EvaluationResult result = members()
                .that(areNoFieldsWithType(List.class))
                .and().haveNameMatching(".*field.*")
                .should().beAnnotatedWith(B.class)
                .andShould().notBePublic()
                .orShould().bePrivate()
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        String failure = getOnlyElement(result.getFailureReport().getDetails());
        assertThat(failure)
                .matches(String.format(".*Field .*%s.* does not have modifier PRIVATE.*", FIELD_D))
                .contains("and Field")
                .matches(String.format(".*Field .*%s.* is not annotated with @B.*", FIELD_D));
    }

    static List<Arguments> restricted_property_rule_ends() {
        String classNameDot = ClassWithVariousMembers.class.getName() + ".";
        return ImmutableList.<Arguments>builder().add(
                        arguments(members().should().haveName(FIELD_A), allMembersExcept(FIELD_A)),
                        arguments(codeUnits().should().haveName(FIELD_A), ALL_CODE_UNIT_DESCRIPTIONS),
                        arguments(fields().should().haveName(FIELD_A), allFieldsExcept(FIELD_A)),
                        arguments(codeUnits().should().haveName("methodA"), allCodeUnitsExcept(METHOD_A)),
                        arguments(methods().should().haveName("methodA"), allMethodsExcept(METHOD_A)),
                        arguments(codeUnits().should().haveName(CONSTRUCTOR_NAME), ALL_METHOD_DESCRIPTIONS),
                        arguments(constructors().should().haveName(CONSTRUCTOR_NAME), emptySet()),
                        arguments(members().should().notHaveName(FIELD_A), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().notHaveName("methodA"), ImmutableSet.of(METHOD_A)),
                        arguments(fields().should().notHaveName(FIELD_A), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().notHaveName("methodA"), ImmutableSet.of(METHOD_A)),
                        arguments(methods().should().notHaveName("methodA"), ImmutableSet.of(METHOD_A)),
                        arguments(codeUnits().should().notHaveName(CONSTRUCTOR_NAME), ALL_CONSTRUCTOR_DESCRIPTIONS),
                        arguments(constructors().should().notHaveName(CONSTRUCTOR_NAME), ALL_CONSTRUCTOR_DESCRIPTIONS),

                        arguments(members().should().haveNameMatching("f.*A"), allMembersExcept(FIELD_A)),
                        arguments(codeUnits().should().haveNameMatching("f.*A"), ALL_CODE_UNIT_DESCRIPTIONS),
                        arguments(fields().should().haveNameMatching("f.*A"), allFieldsExcept(FIELD_A)),
                        arguments(codeUnits().should().haveNameMatching("m.*A"), allCodeUnitsExcept(METHOD_A)),
                        arguments(methods().should().haveNameMatching("m.*A"), allMethodsExcept(METHOD_A)),
                        arguments(codeUnits().should().haveNameMatching(".*init.*"), ALL_METHOD_DESCRIPTIONS),
                        arguments(constructors().should().haveNameMatching(".*init.*"), emptySet()),
                        arguments(members().should().haveNameNotMatching("f.*A"), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().haveNameNotMatching("f.*A"), emptySet()),
                        arguments(fields().should().haveNameNotMatching("f.*A"), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().haveNameNotMatching("m.*A"), ImmutableSet.of(METHOD_A)),
                        arguments(methods().should().haveNameNotMatching("m.*A"), ImmutableSet.of(METHOD_A)),
                        arguments(codeUnits().should().haveNameNotMatching(".*init.*"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                        arguments(constructors().should().haveNameNotMatching(".*init.*"), ALL_CONSTRUCTOR_DESCRIPTIONS),

                        arguments(members().should().haveFullName(classNameDot + FIELD_A), allMembersExcept(FIELD_A)),
                        arguments(fields().should().haveFullName(classNameDot + FIELD_A), allFieldsExcept(FIELD_A)),
                        arguments(codeUnits().should().haveFullName(classNameDot + FIELD_A), ALL_CODE_UNIT_DESCRIPTIONS),
                        arguments(methods().should().haveFullName(classNameDot + METHOD_A), allMethodsExcept(METHOD_A)),
                        arguments(codeUnits().should().haveFullName(classNameDot + METHOD_A), allCodeUnitsExcept(METHOD_A)),
                        arguments(members().should().notHaveFullName(classNameDot + FIELD_A), ImmutableSet.of(FIELD_A)),
                        arguments(fields().should().notHaveFullName(classNameDot + FIELD_A), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().notHaveFullName(classNameDot + FIELD_A), emptySet()),
                        arguments(methods().should().notHaveFullName(classNameDot + METHOD_A), ImmutableSet.of(METHOD_A)),
                        arguments(codeUnits().should().notHaveFullName(classNameDot + METHOD_A), ImmutableSet.of(METHOD_A)),

                        arguments(members().should().haveFullNameMatching(quote(classNameDot) + ".*A\\(?\\)?"), allMembersExcept(FIELD_A, METHOD_A)),
                        arguments(codeUnits().should().haveFullNameMatching(quote(classNameDot) + ".*A"), ALL_CODE_UNIT_DESCRIPTIONS),
                        arguments(fields().should().haveFullNameMatching(quote(classNameDot) + ".*A"), allFieldsExcept(FIELD_A)),
                        arguments(codeUnits().should().haveFullNameMatching(quote(classNameDot) + ".*A" + quote("()")), allCodeUnitsExcept(METHOD_A)),
                        arguments(methods().should().haveFullNameMatching(quote(classNameDot) + ".*A" + quote("()")), allMethodsExcept(METHOD_A)),
                        arguments(codeUnits().should().haveFullNameMatching(quote(classNameDot) + "..*init.*"), ALL_METHOD_DESCRIPTIONS),
                        arguments(constructors().should().haveFullNameMatching(quote(classNameDot) + ".*init.*String\\)"),
                                allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                        arguments(members().should().haveFullNameNotMatching(quote(classNameDot) + ".*A\\(?\\)?"), ImmutableSet.of(FIELD_A, METHOD_A)),
                        arguments(codeUnits().should().haveFullNameNotMatching(quote(classNameDot) + ".*A"), emptySet()),
                        arguments(fields().should().haveFullNameNotMatching(quote(classNameDot) + ".*A"), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().haveFullNameNotMatching(quote(classNameDot) + ".*A" + quote("()")), ImmutableSet.of(METHOD_A)),
                        arguments(methods().should().haveFullNameNotMatching(quote(classNameDot) + ".*A" + quote("()")), ImmutableSet.of(METHOD_A)),
                        arguments(codeUnits().should().haveFullNameNotMatching(quote(classNameDot) + ".*init.*"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                        arguments(constructors().should().haveFullNameNotMatching(quote(classNameDot) + ".*init.*String\\)"), ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),

                        arguments(members().should().haveNameStartingWith("fi"), ALL_CODE_UNIT_DESCRIPTIONS),
                        arguments(fields().should().haveNameStartingWith("m"), ALL_FIELD_DESCRIPTIONS),
                        arguments(codeUnits().should().haveNameStartingWith("<in"), ALL_METHOD_DESCRIPTIONS),
                        arguments(methods().should().haveNameStartingWith("met"), emptySet()),
                        arguments(constructors().should().haveNameStartingWith("c"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                        arguments(members().should().haveNameNotStartingWith("fi"), ALL_FIELD_DESCRIPTIONS),
                        arguments(fields().should().haveNameNotStartingWith("m"), emptySet()),
                        arguments(codeUnits().should().haveNameNotStartingWith("<in"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                        arguments(methods().should().haveNameNotStartingWith("met"), ALL_METHOD_DESCRIPTIONS),
                        arguments(constructors().should().haveNameNotStartingWith("c"), emptySet()),

                        arguments(members().should().haveNameContaining("B"), allMembersExcept(FIELD_B, METHOD_B)),
                        arguments(fields().should().haveNameContaining("A"), allFieldsExcept(FIELD_A)),
                        arguments(codeUnits().should().haveNameContaining("dB"), allCodeUnitsExcept(METHOD_B)),
                        arguments(methods().should().haveNameContaining("D"), allMethodsExcept(METHOD_D)),
                        arguments(constructors().should().haveNameContaining("ni"), emptySet()),
                        arguments(members().should().haveNameNotContaining("B"), ImmutableSet.of(FIELD_B, METHOD_B)),
                        arguments(fields().should().haveNameNotContaining("A"), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().haveNameNotContaining("dB"), ImmutableSet.of(METHOD_B)),
                        arguments(methods().should().haveNameNotContaining("D"), ImmutableSet.of(METHOD_D)),
                        arguments(constructors().should().haveNameNotContaining("ni"), ALL_CONSTRUCTOR_DESCRIPTIONS),

                        arguments(members().should().haveNameEndingWith("C"), allMembersExcept(FIELD_C, METHOD_C)),
                        arguments(fields().should().haveNameEndingWith("dA"), allFieldsExcept(FIELD_A)),
                        arguments(codeUnits().should().haveNameEndingWith("it>"), ALL_METHOD_DESCRIPTIONS),
                        arguments(methods().should().haveNameEndingWith("dC"), allMethodsExcept(METHOD_C)),
                        arguments(constructors().should().haveNameEndingWith("<in"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                        arguments(members().should().haveNameNotEndingWith("C"), ImmutableSet.of(FIELD_C, METHOD_C)),
                        arguments(fields().should().haveNameNotEndingWith("dA"), ImmutableSet.of(FIELD_A)),
                        arguments(codeUnits().should().haveNameNotEndingWith("it>"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                        arguments(methods().should().haveNameNotEndingWith("dC"), ImmutableSet.of(METHOD_C)),
                        arguments(constructors().should().haveNameNotEndingWith("<in"), emptySet()),

                        arguments(members().should().bePublic(), allMembersExcept(
                                FIELD_PUBLIC, METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                        arguments(fields().should().bePublic(), allFieldsExcept(FIELD_PUBLIC)),
                        arguments(codeUnits().should().bePublic(), allCodeUnitsExcept(METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                        arguments(methods().should().bePublic(), allMethodsExcept(METHOD_PUBLIC)),
                        arguments(constructors().should().bePublic(), allConstructorsExcept(CONSTRUCTOR_PUBLIC)),
                        arguments(members().should().notBePublic(),
                                ImmutableSet.of(FIELD_PUBLIC, METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                        arguments(fields().should().notBePublic(), ImmutableSet.of(FIELD_C)),
                        arguments(codeUnits().should().notBePublic(),
                                ImmutableSet.of(METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                        arguments(methods().should().notBePublic(), ImmutableSet.of(METHOD_PUBLIC)),
                        arguments(constructors().should().notBePublic(), ImmutableSet.of(CONSTRUCTOR_PUBLIC)),

                        arguments(members().should().beProtected(), allMembersExcept(
                                FIELD_PROTECTED, METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                        arguments(fields().should().beProtected(), allFieldsExcept(FIELD_PROTECTED)),
                        arguments(codeUnits().should().beProtected(), allCodeUnitsExcept(
                                METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                        arguments(methods().should().beProtected(), allMethodsExcept(METHOD_PROTECTED)),
                        arguments(constructors().should().beProtected(), allConstructorsExcept(CONSTRUCTOR_PROTECTED)),
                        arguments(members().should().notBeProtected(),
                                ImmutableSet.of(FIELD_PROTECTED, METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                        arguments(fields().should().notBeProtected(), ImmutableSet.of(FIELD_PROTECTED)),
                        arguments(codeUnits().should().notBeProtected(),
                                ImmutableSet.of(METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                        arguments(methods().should().notBeProtected(), ImmutableSet.of(METHOD_PROTECTED)),
                        arguments(constructors().should().notBeProtected(), ImmutableSet.of(CONSTRUCTOR_PROTECTED)),

                        arguments(members().should().bePackagePrivate(), allMembersExcept(
                                FIELD_PACKAGE_PRIVATE, METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                        arguments(fields().should().bePackagePrivate(), allFieldsExcept(FIELD_PACKAGE_PRIVATE)),
                        arguments(codeUnits().should().bePackagePrivate(), allCodeUnitsExcept(
                                METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                        arguments(methods().should().bePackagePrivate(), allMethodsExcept(METHOD_PACKAGE_PRIVATE)),
                        arguments(constructors().should().bePackagePrivate(), allConstructorsExcept(CONSTRUCTOR_PACKAGE_PRIVATE)),
                        arguments(members().should().notBePackagePrivate(),
                                ImmutableSet.of(FIELD_PACKAGE_PRIVATE, METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                        arguments(fields().should().notBePackagePrivate(), ImmutableSet.of(FIELD_PACKAGE_PRIVATE)),
                        arguments(codeUnits().should().notBePackagePrivate(),
                                ImmutableSet.of(METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                        arguments(methods().should().notBePackagePrivate(), ImmutableSet.of(METHOD_PACKAGE_PRIVATE)),
                        arguments(constructors().should().notBePackagePrivate(), ImmutableSet.of(CONSTRUCTOR_PACKAGE_PRIVATE)),

                        arguments(members().should().bePrivate(), allMembersExcept(
                                FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(fields().should().bePrivate(), allFieldsExcept(FIELD_PRIVATE)),
                        arguments(codeUnits().should().bePrivate(), allCodeUnitsExcept(
                                METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(methods().should().bePrivate(), allMethodsExcept(METHOD_PRIVATE)),
                        arguments(constructors().should().bePrivate(), allConstructorsExcept(CONSTRUCTOR_PRIVATE)),
                        arguments(members().should().notBePrivate(),
                                ImmutableSet.of(FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(fields().should().notBePrivate(), ImmutableSet.of(FIELD_PRIVATE)),
                        arguments(codeUnits().should().notBePrivate(),
                                ImmutableSet.of(METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(methods().should().notBePrivate(), ImmutableSet.of(METHOD_PRIVATE)),
                        arguments(constructors().should().notBePrivate(), ImmutableSet.of(CONSTRUCTOR_PRIVATE)),

                        arguments(members().should().haveModifier(PRIVATE), allMembersExcept(
                                FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(fields().should().haveModifier(PRIVATE), allFieldsExcept(FIELD_PRIVATE)),
                        arguments(codeUnits().should().haveModifier(PRIVATE), allCodeUnitsExcept(
                                METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(methods().should().haveModifier(PRIVATE), allMethodsExcept(METHOD_PRIVATE)),
                        arguments(constructors().should().haveModifier(PRIVATE), allConstructorsExcept(CONSTRUCTOR_PRIVATE)),
                        arguments(members().should().notHaveModifier(PRIVATE),
                                ImmutableSet.of(FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(fields().should().notHaveModifier(PRIVATE), ImmutableSet.of(FIELD_PRIVATE)),
                        arguments(codeUnits().should().notHaveModifier(PRIVATE),
                                ImmutableSet.of(METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                        arguments(methods().should().notHaveModifier(PRIVATE), ImmutableSet.of(METHOD_PRIVATE)),
                        arguments(constructors().should().notHaveModifier(PRIVATE), ImmutableSet.of(CONSTRUCTOR_PRIVATE))
                )
                .addAll(annotatedWithDataPoints(
                        membersShould -> membersShould.beAnnotatedWith(A.class),
                        membersShould -> membersShould.notBeAnnotatedWith(A.class)))
                .addAll(annotatedWithDataPoints(
                        membersShould -> membersShould.beAnnotatedWith(A.class.getName()),
                        membersShould -> membersShould.notBeAnnotatedWith(A.class.getName())))
                .addAll(annotatedWithDataPoints(
                        membersShould -> membersShould.beAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(A.class))),
                        membersShould -> membersShould.notBeAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(A.class)))))
                .addAll(annotatedWithDataPoints(
                        membersShould -> membersShould.beMetaAnnotatedWith(MetaAnnotation.class),
                        membersShould -> membersShould.notBeMetaAnnotatedWith(MetaAnnotation.class)))
                .addAll(annotatedWithDataPoints(
                        membersShould -> membersShould.beMetaAnnotatedWith(MetaAnnotation.class.getName()),
                        membersShould -> membersShould.notBeMetaAnnotatedWith(MetaAnnotation.class.getName())))
                .addAll(annotatedWithDataPoints(
                        membersShould -> membersShould.beMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(MetaAnnotation.class))),
                        membersShould -> membersShould.notBeMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(MetaAnnotation.class)))))
                .build();
    }

    private static List<Arguments> annotatedWithDataPoints(
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeAnnotatedWithMatchingA,
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeNotAnnotatedWithMatchingA) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areAnnotatedWithA = (Function) makeAnnotatedWithMatchingA;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areNotAnnotatedWithA = (Function) makeNotAnnotatedWithMatchingA;
        return ImmutableList.of(
                arguments(areAnnotatedWithA.apply(members().should()), allMembersExcept(
                        FIELD_ANNOTATED_WITH_A, METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                arguments(areAnnotatedWithA.apply(fields().should()), allFieldsExcept(FIELD_ANNOTATED_WITH_A)),
                arguments(areAnnotatedWithA.apply(codeUnits().should()), allCodeUnitsExcept(
                        METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                arguments(areAnnotatedWithA.apply(methods().should()), allMethodsExcept(METHOD_ANNOTATED_WITH_A)),
                arguments(areAnnotatedWithA.apply(constructors().should()), allConstructorsExcept(CONSTRUCTOR_ANNOTATED_WITH_A)),
                arguments(areNotAnnotatedWithA.apply(members().should()),
                        ImmutableSet.of(FIELD_ANNOTATED_WITH_A, METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                arguments(areNotAnnotatedWithA.apply(fields().should()), ImmutableSet.of(FIELD_ANNOTATED_WITH_A)),
                arguments(areNotAnnotatedWithA.apply(codeUnits().should()),
                        ImmutableSet.of(METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                arguments(areNotAnnotatedWithA.apply(methods().should()), ImmutableSet.of(METHOD_ANNOTATED_WITH_A)),
                arguments(areNotAnnotatedWithA.apply(constructors().should()), ImmutableSet.of(CONSTRUCTOR_ANNOTATED_WITH_A)));
    }

    @ParameterizedTest
    @MethodSource("restricted_property_rule_ends")
    void property_predicates(MembersShouldConjunction<?> conjunction, Set<String> expectedMembers) {
        EvaluationResult result = conjunction
                .evaluate(importClasses(ClassWithVariousMembers.class, A.class, B.class, C.class, MetaAnnotation.class));

        Set<String> actualMembers = parseMembers(result.getFailureReport().getDetails());
        assertThat(actualMembers).hasSameElementsAs(expectedMembers);
    }

    static List<Arguments> restricted_declaration_rule_ends() {
        return ImmutableList.<Arguments>builder()
                .addAll(declaredInDataPoints(
                        membersShould -> membersShould.beDeclaredIn(ClassWithVariousMembers.class),
                        membersShould -> membersShould.notBeDeclaredIn(ClassWithVariousMembers.class)))
                .addAll(declaredInDataPoints(
                        membersShould -> membersShould.beDeclaredIn(ClassWithVariousMembers.class.getName()),
                        membersShould -> membersShould.notBeDeclaredIn(ClassWithVariousMembers.class.getName())))
                .addAll(declaredInDataPoints(
                        membersShould -> membersShould.beDeclaredInClassesThat(equivalentTo(ClassWithVariousMembers.class)),
                        membersShould -> membersShould.beDeclaredInClassesThat(not(equivalentTo(ClassWithVariousMembers.class)))))
                .addAll(declaredInDataPoints(
                        membersShould -> membersShould.beDeclaredInClassesThat().areAssignableTo(ClassWithVariousMembers.class),
                        membersShould -> membersShould.beDeclaredInClassesThat().areNotAssignableTo(ClassWithVariousMembers.class)))
                .build();
    }

    private static List<Arguments> declaredInDataPoints(
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeDeclaredInClassWithVariousMembers,
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeNotDeclaredInClassWithVariousMembers) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areDeclaredInClass = (Function) makeDeclaredInClassWithVariousMembers;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areNotDeclaredInClass = (Function) makeNotDeclaredInClassWithVariousMembers;
        return ImmutableList.of(
                arguments(areDeclaredInClass.apply(members().should()), ALL_OTHER_MEMBER_DESCRIPTIONS),
                arguments(areDeclaredInClass.apply(fields().should()), ALL_OTHER_FIELD_DESCRIPTIONS),
                arguments(areDeclaredInClass.apply(codeUnits().should()), ALL_OTHER_CODE_UNIT_DESCRIPTIONS),
                arguments(areDeclaredInClass.apply(methods().should()), ALL_OTHER_METHOD_DESCRIPTIONS),
                arguments(areDeclaredInClass.apply(constructors().should()), ALL_OTHER_CONSTRUCTOR_DESCRIPTIONS),
                arguments(areNotDeclaredInClass.apply(members().should()), ALL_MEMBER_DESCRIPTIONS),
                arguments(areNotDeclaredInClass.apply(fields().should()), ALL_FIELD_DESCRIPTIONS),
                arguments(areNotDeclaredInClass.apply(codeUnits().should()), ALL_CODE_UNIT_DESCRIPTIONS),
                arguments(areNotDeclaredInClass.apply(methods().should()), ALL_METHOD_DESCRIPTIONS),
                arguments(areNotDeclaredInClass.apply(constructors().should()), ALL_CONSTRUCTOR_DESCRIPTIONS));
    }

    @ParameterizedTest
    @MethodSource("restricted_declaration_rule_ends")
    void declaration_predicates(MembersShouldConjunction<?> conjunction, Set<String> expectedMessages) {
        EvaluationResult result = conjunction
                .evaluate(importClasses(ClassWithVariousMembers.class, OtherClassWithMembers.class));

        Set<String> actualMembers = parseMembers(result.getFailureReport().getDetails());
        assertThat(actualMembers).hasSameElementsAs(expectedMessages);
    }

    static Stream<Arguments> haveNameStartingWith_rules() {
        return Stream.of(
                arguments(members().should().haveNameStartingWith("field"), "field", "violated"),
                arguments(members().should(ArchConditions.haveNameStartingWith("field")), "field", "violated"),
                arguments(fields().should().haveNameStartingWith("field"), "field", "violated"),
                arguments(fields().should(ArchConditions.haveNameStartingWith("field")), "field", "violated"),
                arguments(codeUnits().should().haveNameStartingWith("method"), "method", "violated"),
                arguments(codeUnits().should(ArchConditions.haveNameStartingWith("method")), "method", "violated"),
                arguments(methods().should().haveNameStartingWith("method"), "method", "violated"),
                arguments(methods().should(ArchConditions.haveNameStartingWith("method")), "method", "violated"),
                arguments(constructors().should().haveNameStartingWith("constructor"), "constructor", "<init>"),
                arguments(constructors().should(ArchConditions.haveNameStartingWith("constructor")), "constructor", "<init>")
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameStartingWith_rules")
    void haveNameStartingWith(ArchRule rule, String prefix, String violatingMember) {
        EvaluationResult result = rule.evaluate(importClasses(SimpleFieldAndMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .containsPattern(String.format(".*%s.* does not have name starting with '%s' in %s",
                        quote(violatingMember),
                        quote(prefix),
                        locationPattern(SimpleFieldAndMethod.class)));
    }

    static Stream<Arguments> haveNameNotStartingWith_rules() {
        return Stream.of(
                arguments(members().should().haveNameNotStartingWith("violated"), "violated"),
                arguments(members().should(ArchConditions.haveNameNotStartingWith("violated")), "violated"),
                arguments(fields().should().haveNameNotStartingWith("violated"), "violated"),
                arguments(fields().should(ArchConditions.haveNameNotStartingWith("violated")), "violated"),
                arguments(codeUnits().should().haveNameNotStartingWith("violated"), "violated"),
                arguments(codeUnits().should(ArchConditions.haveNameNotStartingWith("violated")), "violated"),
                arguments(methods().should().haveNameNotStartingWith("violated"), "violated"),
                arguments(methods().should(ArchConditions.haveNameNotStartingWith("violated")), "violated"),
                arguments(constructors().should().haveNameNotStartingWith("<init>"), "<init>"),
                arguments(constructors().should(ArchConditions.haveNameNotStartingWith("<init>")), "<init>")
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameNotStartingWith_rules")
    void haveNameNotStartingWith(ArchRule rule, String prefix) {
        EvaluationResult result = rule.evaluate(importClasses(SimpleFieldAndMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .containsPattern(String.format(".*%s.* has name starting with '%s' in %s",
                        quote(prefix),
                        quote(prefix),
                        locationPattern(SimpleFieldAndMethod.class)));
    }

    static Stream<Arguments> haveNameContaining_rules() {
        return Stream.of(
                arguments(members().should().haveNameContaining("field"), "field", "violated"),
                arguments(members().should(ArchConditions.haveNameContaining("field")), "field", "violated"),
                arguments(fields().should().haveNameContaining("field"), "field", "violated"),
                arguments(fields().should(ArchConditions.haveNameContaining("field")), "field", "violated"),
                arguments(codeUnits().should().haveNameContaining("method"), "method", "violated"),
                arguments(codeUnits().should(ArchConditions.haveNameContaining("method")), "method", "violated"),
                arguments(methods().should().haveNameContaining("method"), "method", "violated"),
                arguments(methods().should(ArchConditions.haveNameContaining("method")), "method", "violated"),
                arguments(constructors().should().haveNameContaining("constructor"), "constructor", "<init>"),
                arguments(constructors().should(ArchConditions.haveNameContaining("constructor")), "constructor", "<init>")
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameContaining_rules")
    void haveNameContaining(ArchRule rule, String infix, String violatingMember) {
        EvaluationResult result = rule.evaluate(importClasses(SimpleFieldAndMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .containsPattern(String.format(".*%s.* does not have name containing '%s' in %s",
                        quote(violatingMember),
                        quote(infix),
                        locationPattern(SimpleFieldAndMethod.class)));
    }

    static Stream<Arguments> haveNameNotContaining_rules() {
        return Stream.of(
                arguments(members().should().haveNameNotContaining("violated"), "violated"),
                arguments(members().should(ArchConditions.haveNameNotContaining("violated")), "violated"),
                arguments(fields().should().haveNameNotContaining("violated"), "violated"),
                arguments(fields().should(ArchConditions.haveNameNotContaining("violated")), "violated"),
                arguments(codeUnits().should().haveNameNotContaining("violated"), "violated"),
                arguments(codeUnits().should(ArchConditions.haveNameNotContaining("violated")), "violated"),
                arguments(methods().should().haveNameNotContaining("violated"), "violated"),
                arguments(methods().should(ArchConditions.haveNameNotContaining("violated")), "violated"),
                arguments(constructors().should().haveNameNotContaining("<init>"), "<init>"),
                arguments(constructors().should(ArchConditions.haveNameNotContaining("<init>")), "<init>")
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameNotContaining_rules")
    void haveNameNotContaining(ArchRule rule, String infix) {
        EvaluationResult result = rule.evaluate(importClasses(SimpleFieldAndMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .containsPattern(String.format(".*%s.* has name containing '%s' in %s",
                        quote(infix),
                        quote(infix),
                        locationPattern(SimpleFieldAndMethod.class)));
    }

    static Stream<Arguments> haveNameEndingWith_rules() {
        return Stream.of(
                arguments(members().should().haveNameEndingWith("field"), "field", "violated"),
                arguments(members().should(ArchConditions.haveNameEndingWith("field")), "field", "violated"),
                arguments(fields().should().haveNameEndingWith("field"), "field", "violated"),
                arguments(fields().should(ArchConditions.haveNameEndingWith("field")), "field", "violated"),
                arguments(codeUnits().should().haveNameEndingWith("method"), "method", "violated"),
                arguments(codeUnits().should(ArchConditions.haveNameEndingWith("method")), "method", "violated"),
                arguments(methods().should().haveNameEndingWith("method"), "method", "violated"),
                arguments(methods().should(ArchConditions.haveNameEndingWith("method")), "method", "violated"),
                arguments(constructors().should().haveNameEndingWith("constructor"), "constructor", "<init>"),
                arguments(constructors().should(ArchConditions.haveNameEndingWith("constructor")), "constructor", "<init>")
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameEndingWith_rules")
    void haveNameEndingWith(ArchRule rule, String suffix, String violatingMember) {
        EvaluationResult result = rule.evaluate(importClasses(SimpleFieldAndMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .containsPattern(String.format(".*%s.* does not have name ending with '%s' in %s",
                        quote(violatingMember),
                        quote(suffix),
                        locationPattern(SimpleFieldAndMethod.class)));
    }

    static Stream<Arguments> haveNameNotEndingWith_rules() {
        return Stream.of(
                arguments(members().should().haveNameNotEndingWith("violated"), "violated"),
                arguments(members().should(ArchConditions.haveNameNotEndingWith("violated")), "violated"),
                arguments(fields().should().haveNameNotEndingWith("violated"), "violated"),
                arguments(fields().should(ArchConditions.haveNameNotEndingWith("violated")), "violated"),
                arguments(codeUnits().should().haveNameNotEndingWith("violated"), "violated"),
                arguments(codeUnits().should(ArchConditions.haveNameNotEndingWith("violated")), "violated"),
                arguments(methods().should().haveNameNotEndingWith("violated"), "violated"),
                arguments(methods().should(ArchConditions.haveNameNotEndingWith("violated")), "violated"),
                arguments(constructors().should().haveNameNotEndingWith("<init>"), "<init>"),
                arguments(constructors().should(ArchConditions.haveNameNotEndingWith("<init>")), "<init>")
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameNotEndingWith_rules")
    void haveNameNotEndingWith(ArchRule rule, String suffix) {
        EvaluationResult result = rule.evaluate(importClasses(SimpleFieldAndMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .containsPattern(String.format(".*%s.* has name ending with '%s' in %s",
                        quote(suffix),
                        quote(suffix),
                        locationPattern(SimpleFieldAndMethod.class)));
    }

    @Test
    public void containNumberOfElements_passes_on_matching_predicate() {
        assertThatRule(members().should().containNumberOfElements(alwaysTrue()))
                .checking(importClasses(SimpleFieldAndMethod.class))
                .hasNoViolation();
    }

    @Test
    public void containNumberOfElements_fails_on_mismatching_predicate() {
        assertThatRule(members().should().containNumberOfElements(alwaysFalse()))
                .checking(importClasses(SimpleFieldAndMethod.class))
                .hasOnlyViolations("there is/are 3 element(s) in ["
                        + "com.tngtech.archunit.lang.syntax.elements.testclasses.SimpleFieldAndMethod.<init>(), "
                        + "com.tngtech.archunit.lang.syntax.elements.testclasses.SimpleFieldAndMethod.violated, "
                        + "com.tngtech.archunit.lang.syntax.elements.testclasses.SimpleFieldAndMethod.violated()]");
    }

    @Test
    public void should_fail_on_empty_should_by_default() {
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                ruleWithEmptyShould().check(new ClassFileImporter().importClasses(getClass()));
            }
        }).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failed to check any classes");
    }

    @Test
    public void should_allow_empty_should_if_configured() {
        archConfiguration.setFailOnEmptyShould(false);

        ruleWithEmptyShould().check(new ClassFileImporter().importClasses(getClass()));
    }

    @Test
    public void should_allow_empty_should_if_overridden_by_rule() {
        archConfiguration.setFailOnEmptyShould(true);

        ruleWithEmptyShould().allowEmptyShould(true).check(new ClassFileImporter().importClasses(getClass()));
    }

    private static ArchRule ruleWithEmptyShould() {
        return members().that(alwaysFalse()).should().bePublic();
    }

    private Set<String> parseMembers(List<String> details) {
        return parseMembers(ImmutableList.of(ClassWithVariousMembers.class, OtherClassWithMembers.class), details);
    }

    static Set<String> parseMembers(Class<?> possibleOwner, List<String> details) {
        return parseMembers(ImmutableList.of(possibleOwner), details);
    }

    static Set<String> parseMembers(List<Class<?>> possibleOwners, List<String> details) {
        List<String> classNamePatterns = formatNamesOf(possibleOwners).stream().map(Pattern::quote).collect(toList());
        String classesWithMembersRegex = String.format("(?:%s)", Joiner.on("|").join(classNamePatterns));
        return details.stream()
                .map(detail -> detail
                        .replaceAll(
                                String.format("Field <%s\\.([^:]+)> .*", classesWithMembersRegex),
                                "$1")
                        .replaceAll(
                                String.format("(?:Method|Constructor) <%s\\.([^:]+\\))> .*", classesWithMembersRegex),
                                "$1"))
                .collect(toSet());
    }
}
