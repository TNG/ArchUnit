package com.tngtech.archunit.lang.syntax.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.A;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.B;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.C;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ClassWithVariousMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.MetaAnnotation;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.OtherClassWithMembers;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
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
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_C;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_D;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PACKAGE_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PRIVATE;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PROTECTED;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.FIELD_PUBLIC;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_A;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.METHOD_ANNOTATED_WITH_A;
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
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.Collections.emptySet;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MembersShouldTest {

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

    @DataProvider
    public static Object[][] restricted_property_rule_ends() {
        String classNameDot = ClassWithVariousMembers.class.getName() + ".";
        ImmutableList.Builder<Object[]> data = ImmutableList.<Object[]>builder().add(
                $(members().should().haveName(FIELD_A), allMembersExcept(FIELD_A)),
                $(codeUnits().should().haveName(FIELD_A), ALL_CODE_UNIT_DESCRIPTIONS),
                $(fields().should().haveName(FIELD_A), allFieldsExcept(FIELD_A)),
                $(codeUnits().should().haveName("methodA"), allCodeUnitsExcept(METHOD_A)),
                $(methods().should().haveName("methodA"), allMethodsExcept(METHOD_A)),
                $(codeUnits().should().haveName(CONSTRUCTOR_NAME), ALL_METHOD_DESCRIPTIONS),
                $(constructors().should().haveName(CONSTRUCTOR_NAME), emptySet()),
                $(members().should().notHaveName(FIELD_A), ImmutableSet.of(FIELD_A)),
                $(codeUnits().should().notHaveName("methodA"), ImmutableSet.of(METHOD_A)),
                $(fields().should().notHaveName(FIELD_A), ImmutableSet.of(FIELD_A)),
                $(codeUnits().should().notHaveName("methodA"), ImmutableSet.of(METHOD_A)),
                $(methods().should().notHaveName("methodA"), ImmutableSet.of(METHOD_A)),
                $(codeUnits().should().notHaveName(CONSTRUCTOR_NAME), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(constructors().should().notHaveName(CONSTRUCTOR_NAME), ALL_CONSTRUCTOR_DESCRIPTIONS),

                $(members().should().haveNameMatching("f.*A"), allMembersExcept(FIELD_A)),
                $(codeUnits().should().haveNameMatching("f.*A"), ALL_CODE_UNIT_DESCRIPTIONS),
                $(fields().should().haveNameMatching("f.*A"), allFieldsExcept(FIELD_A)),
                $(codeUnits().should().haveNameMatching("m.*A"), allCodeUnitsExcept(METHOD_A)),
                $(methods().should().haveNameMatching("m.*A"), allMethodsExcept(METHOD_A)),
                $(codeUnits().should().haveNameMatching(".*init.*"), ALL_METHOD_DESCRIPTIONS),
                $(constructors().should().haveNameMatching(".*init.*"), emptySet()),
                $(members().should().haveNameNotMatching("f.*A"), ImmutableSet.of(FIELD_A)),
                $(codeUnits().should().haveNameNotMatching("f.*A"), emptySet()),
                $(fields().should().haveNameNotMatching("f.*A"), ImmutableSet.of(FIELD_A)),
                $(codeUnits().should().haveNameNotMatching("m.*A"), ImmutableSet.of(METHOD_A)),
                $(methods().should().haveNameNotMatching("m.*A"), ImmutableSet.of(METHOD_A)),
                $(codeUnits().should().haveNameNotMatching(".*init.*"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(constructors().should().haveNameNotMatching(".*init.*"), ALL_CONSTRUCTOR_DESCRIPTIONS),

                $(members().should().haveFullName(classNameDot + FIELD_A), allMembersExcept(FIELD_A)),
                $(fields().should().haveFullName(classNameDot + FIELD_A), allFieldsExcept(FIELD_A)),
                $(codeUnits().should().haveFullName(classNameDot + FIELD_A), ALL_CODE_UNIT_DESCRIPTIONS),
                $(methods().should().haveFullName(classNameDot + METHOD_A), allMethodsExcept(METHOD_A)),
                $(codeUnits().should().haveFullName(classNameDot + METHOD_A), allCodeUnitsExcept(METHOD_A)),
                $(members().should().notHaveFullName(classNameDot + FIELD_A), ImmutableSet.of(FIELD_A)),
                $(fields().should().notHaveFullName(classNameDot + FIELD_A), ImmutableSet.of(FIELD_A)),
                $(codeUnits().should().notHaveFullName(classNameDot + FIELD_A), emptySet()),
                $(methods().should().notHaveFullName(classNameDot + METHOD_A), ImmutableSet.of(METHOD_A)),
                $(codeUnits().should().notHaveFullName(classNameDot + METHOD_A), ImmutableSet.of(METHOD_A)),

                $(members().should().haveFullNameMatching(quote(classNameDot) + ".*A\\(?\\)?"), allMembersExcept(FIELD_A, METHOD_A)),
                $(codeUnits().should().haveFullNameMatching(quote(classNameDot) + ".*A"), ALL_CODE_UNIT_DESCRIPTIONS),
                $(fields().should().haveFullNameMatching(quote(classNameDot) + ".*A"), allFieldsExcept(FIELD_A)),
                $(codeUnits().should().haveFullNameMatching(quote(classNameDot) + ".*A" + quote("()")), allCodeUnitsExcept(METHOD_A)),
                $(methods().should().haveFullNameMatching(quote(classNameDot) + ".*A" + quote("()")), allMethodsExcept(METHOD_A)),
                $(codeUnits().should().haveFullNameMatching(quote(classNameDot) + "..*init.*"), ALL_METHOD_DESCRIPTIONS),
                $(constructors().should().haveFullNameMatching(quote(classNameDot) + ".*init.*String\\)"),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(members().should().haveFullNameNotMatching(quote(classNameDot) + ".*A\\(?\\)?"), ImmutableSet.of(FIELD_A, METHOD_A)),
                $(codeUnits().should().haveFullNameNotMatching(quote(classNameDot) + ".*A"), emptySet()),
                $(fields().should().haveFullNameNotMatching(quote(classNameDot) + ".*A"), ImmutableSet.of(FIELD_A)),
                $(codeUnits().should().haveFullNameNotMatching(quote(classNameDot) + ".*A" + quote("()")), ImmutableSet.of(METHOD_A)),
                $(methods().should().haveFullNameNotMatching(quote(classNameDot) + ".*A" + quote("()")), ImmutableSet.of(METHOD_A)),
                $(codeUnits().should().haveFullNameNotMatching(quote(classNameDot) + ".*init.*"), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(constructors().should().haveFullNameNotMatching(quote(classNameDot) + ".*init.*String\\)"), ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),

                $(members().should().bePublic(), allMembersExcept(
                        FIELD_PUBLIC, METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(fields().should().bePublic(), allFieldsExcept(FIELD_PUBLIC)),
                $(codeUnits().should().bePublic(), allCodeUnitsExcept(METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(methods().should().bePublic(), allMethodsExcept(METHOD_PUBLIC)),
                $(constructors().should().bePublic(), allConstructorsExcept(CONSTRUCTOR_PUBLIC)),
                $(members().should().notBePublic(),
                        ImmutableSet.of(FIELD_PUBLIC, METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(fields().should().notBePublic(), ImmutableSet.of(FIELD_C)),
                $(codeUnits().should().notBePublic(),
                        ImmutableSet.of(METHOD_PUBLIC, CONSTRUCTOR_PUBLIC)),
                $(methods().should().notBePublic(), ImmutableSet.of(METHOD_PUBLIC)),
                $(constructors().should().notBePublic(), ImmutableSet.of(CONSTRUCTOR_PUBLIC)),

                $(members().should().beProtected(), allMembersExcept(
                        FIELD_PROTECTED, METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(fields().should().beProtected(), allFieldsExcept(FIELD_PROTECTED)),
                $(codeUnits().should().beProtected(), allCodeUnitsExcept(
                        METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(methods().should().beProtected(), allMethodsExcept(METHOD_PROTECTED)),
                $(constructors().should().beProtected(), allConstructorsExcept(CONSTRUCTOR_PROTECTED)),
                $(members().should().notBeProtected(),
                        ImmutableSet.of(FIELD_PROTECTED, METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(fields().should().notBeProtected(), ImmutableSet.of(FIELD_PROTECTED)),
                $(codeUnits().should().notBeProtected(),
                        ImmutableSet.of(METHOD_PROTECTED, CONSTRUCTOR_PROTECTED)),
                $(methods().should().notBeProtected(), ImmutableSet.of(METHOD_PROTECTED)),
                $(constructors().should().notBeProtected(), ImmutableSet.of(CONSTRUCTOR_PROTECTED)),

                $(members().should().bePackagePrivate(), allMembersExcept(
                        FIELD_PACKAGE_PRIVATE, METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(fields().should().bePackagePrivate(), allFieldsExcept(FIELD_PACKAGE_PRIVATE)),
                $(codeUnits().should().bePackagePrivate(), allCodeUnitsExcept(
                        METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(methods().should().bePackagePrivate(), allMethodsExcept(METHOD_PACKAGE_PRIVATE)),
                $(constructors().should().bePackagePrivate(), allConstructorsExcept(CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(members().should().notBePackagePrivate(),
                        ImmutableSet.of(FIELD_PACKAGE_PRIVATE, METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(fields().should().notBePackagePrivate(), ImmutableSet.of(FIELD_PACKAGE_PRIVATE)),
                $(codeUnits().should().notBePackagePrivate(),
                        ImmutableSet.of(METHOD_PACKAGE_PRIVATE, CONSTRUCTOR_PACKAGE_PRIVATE)),
                $(methods().should().notBePackagePrivate(), ImmutableSet.of(METHOD_PACKAGE_PRIVATE)),
                $(constructors().should().notBePackagePrivate(), ImmutableSet.of(CONSTRUCTOR_PACKAGE_PRIVATE)),

                $(members().should().bePrivate(), allMembersExcept(
                        FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(fields().should().bePrivate(), allFieldsExcept(FIELD_PRIVATE)),
                $(codeUnits().should().bePrivate(), allCodeUnitsExcept(
                        METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(methods().should().bePrivate(), allMethodsExcept(METHOD_PRIVATE)),
                $(constructors().should().bePrivate(), allConstructorsExcept(CONSTRUCTOR_PRIVATE)),
                $(members().should().notBePrivate(),
                        ImmutableSet.of(FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(fields().should().notBePrivate(), ImmutableSet.of(FIELD_PRIVATE)),
                $(codeUnits().should().notBePrivate(),
                        ImmutableSet.of(METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(methods().should().notBePrivate(), ImmutableSet.of(METHOD_PRIVATE)),
                $(constructors().should().notBePrivate(), ImmutableSet.of(CONSTRUCTOR_PRIVATE)),

                $(members().should().haveModifier(PRIVATE), allMembersExcept(
                        FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(fields().should().haveModifier(PRIVATE), allFieldsExcept(FIELD_PRIVATE)),
                $(codeUnits().should().haveModifier(PRIVATE), allCodeUnitsExcept(
                        METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(methods().should().haveModifier(PRIVATE), allMethodsExcept(METHOD_PRIVATE)),
                $(constructors().should().haveModifier(PRIVATE), allConstructorsExcept(CONSTRUCTOR_PRIVATE)),
                $(members().should().notHaveModifier(PRIVATE),
                        ImmutableSet.of(FIELD_PRIVATE, METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(fields().should().notHaveModifier(PRIVATE), ImmutableSet.of(FIELD_PRIVATE)),
                $(codeUnits().should().notHaveModifier(PRIVATE),
                        ImmutableSet.of(METHOD_PRIVATE, CONSTRUCTOR_PRIVATE)),
                $(methods().should().notHaveModifier(PRIVATE), ImmutableSet.of(METHOD_PRIVATE)),
                $(constructors().should().notHaveModifier(PRIVATE), ImmutableSet.of(CONSTRUCTOR_PRIVATE)));

        data.add(annotatedWithDataPoints(
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.beAnnotatedWith(A.class);
                    }
                },
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.notBeAnnotatedWith(A.class);
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.beAnnotatedWith(A.class.getName());
                    }
                },
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.notBeAnnotatedWith(A.class.getName());
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.beAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(A.class)));
                    }
                },
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.notBeAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(A.class)));
                    }
                }));

        data.add(annotatedWithDataPoints(
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.beMetaAnnotatedWith(MetaAnnotation.class);
                    }
                },
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.notBeMetaAnnotatedWith(MetaAnnotation.class);
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.beMetaAnnotatedWith(MetaAnnotation.class.getName());
                    }
                },
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.notBeMetaAnnotatedWith(MetaAnnotation.class.getName());
                    }
                }));
        data.add(annotatedWithDataPoints(
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.beMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(MetaAnnotation.class)));
                    }
                },
                new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                    @Override
                    public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                        return membersShould.notBeMetaAnnotatedWith(GET_RAW_TYPE.is(equivalentTo(MetaAnnotation.class)));
                    }
                }));
        return data.build().toArray(new Object[0][]);
    }

    private static Object[][] annotatedWithDataPoints(
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeAnnotatedWithMatchingA,
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeNotAnnotatedWithMatchingA) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areAnnotatedWithA = (Function) makeAnnotatedWithMatchingA;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areNotAnnotatedWithA = (Function) makeNotAnnotatedWithMatchingA;
        return $$(
                $(areAnnotatedWithA.apply(members().should()), allMembersExcept(
                        FIELD_ANNOTATED_WITH_A, METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(areAnnotatedWithA.apply(fields().should()), allFieldsExcept(FIELD_ANNOTATED_WITH_A)),
                $(areAnnotatedWithA.apply(codeUnits().should()), allCodeUnitsExcept(
                        METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(areAnnotatedWithA.apply(methods().should()), allMethodsExcept(METHOD_ANNOTATED_WITH_A)),
                $(areAnnotatedWithA.apply(constructors().should()), allConstructorsExcept(CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(areNotAnnotatedWithA.apply(members().should()),
                        ImmutableSet.of(FIELD_ANNOTATED_WITH_A, METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(areNotAnnotatedWithA.apply(fields().should()), ImmutableSet.of(FIELD_ANNOTATED_WITH_A)),
                $(areNotAnnotatedWithA.apply(codeUnits().should()),
                        ImmutableSet.of(METHOD_ANNOTATED_WITH_A, CONSTRUCTOR_ANNOTATED_WITH_A)),
                $(areNotAnnotatedWithA.apply(methods().should()), ImmutableSet.of(METHOD_ANNOTATED_WITH_A)),
                $(areNotAnnotatedWithA.apply(constructors().should()), ImmutableSet.of(CONSTRUCTOR_ANNOTATED_WITH_A)));
    }

    @Test
    @UseDataProvider("restricted_property_rule_ends")
    public void property_predicates(MembersShouldConjunction<?> conjunction, Set<String> expectedMembers) {
        EvaluationResult result = conjunction
                .evaluate(importClasses(ClassWithVariousMembers.class, A.class, B.class, C.class, MetaAnnotation.class));

        Set<String> actualMembers = parseMembers(result.getFailureReport().getDetails());
        assertThat(actualMembers).containsOnlyElementsOf(expectedMembers);
    }

    @DataProvider
    public static Object[][] restricted_declaration_rule_ends() {
        return ImmutableList.<Object[]>builder().add(
                declaredInDataPoints(
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.beDeclaredIn(ClassWithVariousMembers.class);
                            }
                        },
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.notBeDeclaredIn(ClassWithVariousMembers.class);
                            }
                        }
                )).add(
                declaredInDataPoints(
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.beDeclaredIn(ClassWithVariousMembers.class.getName());
                            }
                        },
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.notBeDeclaredIn(ClassWithVariousMembers.class.getName());
                            }
                        }
                )).add(
                declaredInDataPoints(
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.beDeclaredInClassesThat(equivalentTo(ClassWithVariousMembers.class));
                            }
                        },
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.beDeclaredInClassesThat(not(equivalentTo(ClassWithVariousMembers.class)));
                            }
                        }
                )).add(
                declaredInDataPoints(
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.beDeclaredInClassesThat().areAssignableTo(ClassWithVariousMembers.class);
                            }
                        },
                        new Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>>() {
                            @Override
                            public MembersShouldConjunction<?> apply(MembersShould<MembersShouldConjunction<?>> membersShould) {
                                return membersShould.beDeclaredInClassesThat().areNotAssignableTo(ClassWithVariousMembers.class);
                            }
                        }
                )).build().toArray(new Object[0][]);
    }

    private static Object[][] declaredInDataPoints(
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeDeclaredInClassWithVariousMembers,
            Function<MembersShould<MembersShouldConjunction<?>>, MembersShouldConjunction<?>> makeNotDeclaredInClassWithVariousMembers) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areDeclaredInClass = (Function) makeDeclaredInClassWithVariousMembers;
        @SuppressWarnings({"unchecked", "rawtypes"})
        Function<MembersShould<?>, MembersShouldConjunction<?>> areNotDeclaredInClass = (Function) makeNotDeclaredInClassWithVariousMembers;
        return $$(
                $(areDeclaredInClass.apply(members().should()), ALL_OTHER_MEMBER_DESCRIPTIONS),
                $(areDeclaredInClass.apply(fields().should()), ALL_OTHER_FIELD_DESCRIPTIONS),
                $(areDeclaredInClass.apply(codeUnits().should()), ALL_OTHER_CODE_UNIT_DESCRIPTIONS),
                $(areDeclaredInClass.apply(methods().should()), ALL_OTHER_METHOD_DESCRIPTIONS),
                $(areDeclaredInClass.apply(constructors().should()), ALL_OTHER_CONSTRUCTOR_DESCRIPTIONS),
                $(areNotDeclaredInClass.apply(members().should()), ALL_MEMBER_DESCRIPTIONS),
                $(areNotDeclaredInClass.apply(fields().should()), ALL_FIELD_DESCRIPTIONS),
                $(areNotDeclaredInClass.apply(codeUnits().should()), ALL_CODE_UNIT_DESCRIPTIONS),
                $(areNotDeclaredInClass.apply(methods().should()), ALL_METHOD_DESCRIPTIONS),
                $(areNotDeclaredInClass.apply(constructors().should()), ALL_CONSTRUCTOR_DESCRIPTIONS));
    }

    @Test
    @UseDataProvider("restricted_declaration_rule_ends")
    public void declaration_predicates(MembersShouldConjunction<?> conjunction, Set<String> expectedMessages) {
        EvaluationResult result = conjunction
                .evaluate(importClasses(ClassWithVariousMembers.class, OtherClassWithMembers.class));

        Set<String> actualMembers = parseMembers(result.getFailureReport().getDetails());
        assertThat(actualMembers).containsOnlyElementsOf(expectedMessages);
    }

    private Set<String> parseMembers(List<String> details) {
        return parseMembers(ImmutableList.of(ClassWithVariousMembers.class, OtherClassWithMembers.class), details);
    }

    static Set<String> parseMembers(Class<?> possibleOwner, List<String> details) {
        return parseMembers(ImmutableList.<Class<?>>of(possibleOwner), details);
    }

    static Set<String> parseMembers(List<Class<?>> possibleOwners, List<String> details) {
        List<String> classNamePatterns = new ArrayList<>();
        for (String className : namesOf(possibleOwners)) {
            classNamePatterns.add(quote(className));
        }
        String classesWithMembersRegex = String.format("(?:%s)", Joiner.on("|").join(classNamePatterns));
        Set<String> result = new HashSet<>();
        for (String detail : details) {
            result.add(detail
                    .replaceAll(
                            String.format("Field <%s\\.([^:]+)> .*", classesWithMembersRegex),
                            "$1")
                    .replaceAll(
                            String.format("(?:Method|Constructor) <%s\\.([^:]+\\))> .*", classesWithMembersRegex),
                            "$1"));
        }
        return result;
    }
}
