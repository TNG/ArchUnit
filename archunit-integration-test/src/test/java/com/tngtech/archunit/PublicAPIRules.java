package com.tngtech.archunit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.lang.syntax.ClassesIdentityTransformer;

import static com.tngtech.archunit.ArchUnitArchitectureTest.THIRDPARTY_PACKAGE_IDENTIFIER;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ANONYMOUS_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static java.util.Arrays.stream;

public class PublicAPIRules {
    @ArchTest
    public static final ArchRule only_public_API_classes_or_classes_explicitly_marked_as_internal_are_accessible =
            classes()
                    .that(are(not(publicAPI())))
                    .and(are(not(internal())))
                    .and().areNotAssignableTo(Annotation.class)
                    .and(are(not(enclosedInANonPublicClass())))
                    .and().resideOutsideOfPackage(THIRDPARTY_PACKAGE_IDENTIFIER)

                    .should().notBePublic()

                    .as("classes that are not explicitly designed as API should not be public")
                    .because("we risk extensibility and maintainability of ArchUnit, if internal classes leak to users");

    @ArchTest
    public static final ArchRule only_members_that_are_public_API_or_explicitly_marked_as_internal_are_accessible =
            members()
                    .that(are(withoutAPIMarking()))
                    .and(doNot(inheritPublicAPI()))
                    .and(are(relevantArchUnitMembers()))

                    .should(notBePublic())

                    .because("users of ArchUnit should only access intended members, to preserve maintainability");

    @ArchTest
    public static final ArchRule all_public_classes_that_are_not_meant_for_inheritance_or_internal_are_final =
            classes()
                    .that().arePublic()
                    .and(haveAPublicConstructor())
                    .and(are(not(internal())))
                    .and(are(not(enclosedInANonPublicClass())))
                    .and().resideOutsideOfPackage(THIRDPARTY_PACKAGE_IDENTIFIER)
                    .and(are(not(equivalentTo(ArchUnitRunner.class))))

                    .should(bePublicAPIForInheritance())
                    .orShould(beInterfaces())
                    .orShould().haveModifier(FINAL)

                    .as("all public classes not meant for inheritance should be final")
                    .because("users of ArchUnit should only inherit from intended classes, to preserve maintainability");

    @ArchTest
    public static final ArchRule only_entry_point_and_syntax_interfaces_should_be_public =
            classes()
                    .that().resideInAPackage("..syntax..")
                    .and().haveNameNotMatching(".*" + ArchRuleDefinition.class.getSimpleName() + ".*")
                    // FIXME: Remove this line once we throw the deprecated class out of the public API
                    .and().doNotHaveFullyQualifiedName(ClassesIdentityTransformer.class.getName())
                    .and().areNotInterfaces()
                    .and().areNotAnnotatedWith(Internal.class)
                    .should().notBePublic()
                    .as(String.format(
                            "Only %s and interfaces within the ArchUnit syntax (..syntax..) should be public",
                            ArchRuleDefinition.class.getSimpleName()));

    @ArchTest
    public static final ArchRule parameters_of_public_API_are_public =
            codeUnits()
                    .that().areDeclaredInClassesThat().arePublic()
                    .and().areDeclaredInClassesThat().areNotAnnotatedWith(Internal.class)
                    .and().arePublic()
                    .and().doNotHaveName("adhereToPlantUmlDiagram")
                    .should().haveRawParameterTypes(thatArePublic());

    @ArchTest
    public static final ArchRule predicate_parameters_of_public_API_code_units_should_be_contravariant =
            codeUnits()
                    .that().haveRawParameterTypes(anyElementThat(is(assignableTo(DescribedPredicate.class))))
                    .and(are(declaredIn(modifier(PUBLIC))))
                    .and(are(not(declaredIn(annotatedWith(Internal.class)))))
                    .and(have(modifier(PUBLIC)))
                    .should(haveContravariantPredicateParameterTypes())
                    .as(String.format(
                            "Public API methods that take a %s<PARAM> should declare the type parameter contravariantly (i.e. %s<? super PARAM>)",
                            DescribedPredicate.class.getSimpleName(), DescribedPredicate.class.getSimpleName()))
                    .because(String.format(
                            "any predicate for a super type is also a predicate for a subtype (otherwise one could for example not pass `%s.name(\"foo\")` to a method taking `%s<JavaClass>`)",
                            HasName.Predicates.class.getName(), DescribedPredicate.class.getSimpleName()));

    @ArchTest
    public static final ArchRule Guava_should_not_leak_into_public_API =
            CompositeArchRule.of(
                    classes().that(publicAPI()).should().notBeAssignableTo(guavaClass()))
                    .and(codeUnits()
                            .that().arePublic()
                            .and().areDeclaredInClassesThat(are(publicAPI()))
                            .should().haveRawParameterTypes(withoutGuava())
                            .andShould().haveRawReturnType(not(guavaClass()).as("that are no Guava types")));

    private static DescribedPredicate<JavaClass> publicAPI() {
        return annotatedWith(PublicAPI.class).<JavaClass>forSubType()
                .or(haveMemberThatBelongsToPublicApi())
                .or(markedAsPublicAPIForInheritance());
    }

    private static DescribedPredicate<JavaClass> internal() {
        return annotatedWith(Internal.class).<JavaClass>forSubType()
                .or(equivalentTo(Internal.class));
    }

    private static DescribedPredicate<JavaClass> enclosedInANonPublicClass() {
        return new DescribedPredicate<JavaClass>("enclosed in a non-public class") {
            @Override
            public boolean apply(JavaClass input) {
                return input.getEnclosingClass().isPresent() &&
                        !input.getEnclosingClass().get().getModifiers().contains(PUBLIC);
            }
        };
    }

    private static DescribedPredicate<JavaMember> inheritedFromObjectOrEnum() {
        return new DescribedPredicate<JavaMember>("inherited from %s or %s",
                Object.class.getName(), Enum.class.getName()) {

            @Override
            public boolean apply(JavaMember input) {
                if (!(input instanceof JavaMethod)) {
                    return false;
                }

                JavaMethod methodToCheck = (JavaMethod) input;
                return equivalentMethod(methodToCheck, "toString") ||
                        equivalentMethod(methodToCheck, "hashCode") ||
                        equivalentMethod(methodToCheck, "equals", Object.class) ||
                        enumMethod(methodToCheck, "values") ||
                        enumMethod(methodToCheck, "valueOf", String.class);
            }

            private boolean equivalentMethod(JavaMethod method, String name, Class<?>... paramTypes) {
                return method.getName().equals(name) &&
                        method.getRawParameterTypes().getNames().equals(JavaClass.namesOf(paramTypes));
            }

            private boolean enumMethod(JavaMethod methodToCheck, String name, Class<?>... paramTypes) {
                return methodToCheck.getOwner().isAssignableTo(Enum.class)
                        && equivalentMethod(methodToCheck, name, paramTypes);
            }
        };
    }

    private static DescribedPredicate<JavaMember> declaredInClassIn(String packageIdentifier) {
        return declaredIn(resideInAPackage(packageIdentifier).as("class in '%s'", packageIdentifier));
    }

    private static ArchCondition<JavaMember> notBePublic() {
        return new ArchCondition<JavaMember>("not be public") {
            @Override
            public void check(JavaMember member, ConditionEvents events) {
                boolean satisfied = !member.getModifiers().contains(PUBLIC);
                events.add(new SimpleConditionEvent(member, satisfied,
                        String.format("member %s.%s is %spublic in %s",
                                member.getOwner().getName(),
                                member.getName(),
                                satisfied ? "not " : "",
                                member.getSourceCodeLocation())));
            }
        };
    }

    private static DescribedPredicate<JavaClass> haveAPublicConstructor() {
        return new DescribedPredicate<JavaClass>("have a public constructor") {
            @Override
            public boolean apply(JavaClass input) {
                for (JavaConstructor constructor : input.getConstructors()) {
                    if (constructor.getModifiers().contains(PUBLIC)) {
                        return true;
                    }
                }
                return input.getConstructors().isEmpty() &&
                        input.getSuperClass().isPresent() &&
                        haveAPublicConstructor().apply(input.getSuperClass().get());
            }
        };
    }

    private static DescribedPredicate<JavaClass> haveMemberThatBelongsToPublicApi() {
        return new DescribedPredicate<JavaClass>("have member that belongs to public API") {
            @Override
            public boolean apply(JavaClass input) {
                for (JavaMember member : input.getAllMembers()) {
                    if (member.isAnnotatedWith(PublicAPI.class)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static DescribedPredicate<JavaMember> withoutAPIMarking() {
        return not(annotatedWith(PublicAPI.class)).<JavaMember>forSubType()
                .and(not(annotatedWith(Internal.class)).forSubType())
                .and(declaredIn(modifier(PUBLIC)))
                .as("without API marking");
    }

    private static DescribedPredicate<JavaMember> inheritPublicAPI() {
        return new DescribedPredicate<JavaMember>("inherit public API") {
            @Override
            public boolean apply(JavaMember input) {
                return declaredIn(markedAsPublicAPIForInheritance()).apply(input) ||
                        inheritsFromSuperMethod(input);
            }

            private boolean inheritsFromSuperMethod(JavaMember input) {
                if (!(input instanceof JavaMethod)) {
                    return false;
                }

                JavaMethod methodToCheck = (JavaMethod) input;
                for (JavaMethod candidate : input.getOwner().getAllMethods()) {
                    if (isPublicAPISuperMethod(candidate, methodToCheck)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean isPublicAPISuperMethod(JavaMethod candidate, JavaMethod methodToCheck) {
                return candidate.getName().equals(methodToCheck.getName()) &&
                        candidate.getRawParameterTypes().equals(methodToCheck.getRawParameterTypes()) &&
                        candidate.isAnnotatedWith(PublicAPI.class);
            }
        };
    }

    private static DescribedPredicate<JavaMember> relevantArchUnitMembers() {
        return not(inheritedFromObjectOrEnum())
                .and(not(declaredIn(assignableTo(Annotation.class))))
                .and(not(declaredIn(ANONYMOUS_CLASSES)))
                .and(not(declaredIn(internal())))
                .and(not(declaredInClassIn(THIRDPARTY_PACKAGE_IDENTIFIER)))
                .as("relevant members");
    }

    private static DescribedPredicate<JavaClass> markedAsPublicAPIForInheritance() {
        return new DescribedPredicate<JavaClass>("inherit public API") {
            @Override
            public boolean apply(JavaClass input) {
                for (JavaClass clazz : input.getAllClassesSelfIsAssignableTo()) {
                    if (clazz.isAnnotatedWith(publicApiForInheritance())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static DescribedPredicate<JavaAnnotation<?>> publicApiForInheritance() {
        return new DescribedPredicate<JavaAnnotation<?>>("@%s(usage = %s)", PublicAPI.class.getSimpleName(), INHERITANCE) {
            @Override
            public boolean apply(JavaAnnotation<?> input) {
                return input.getRawType().isEquivalentTo(PublicAPI.class) &&
                        input.as(PublicAPI.class).usage() == INHERITANCE;
            }
        };
    }

    private static ArchCondition<? super JavaClass> beInterfaces() {
        return new ArchCondition<JavaClass>("be interfaces") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = item.isInterface();
                events.add(new SimpleConditionEvent(item, satisfied,
                        String.format("class %s is %sinterface", item.getName(), satisfied ? "" : "no ")));
            }
        };
    }

    private static ArchCondition<JavaClass> bePublicAPIForInheritance() {
        return new ArchCondition<JavaClass>("be public API for inheritance") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = item.isAnnotatedWith(publicApiForInheritance()) ||
                        markedAsPublicAPIForInheritance().apply(item);
                events.add(new SimpleConditionEvent(item, satisfied,
                        String.format("class %s is %smeant for inheritance", item.getName(), satisfied ? "" : "not ")));
            }
        };
    }

    private static DescribedPredicate<List<JavaClass>> thatArePublic() {
        return new DescribedPredicate<List<JavaClass>>("that are public") {
            @Override
            public boolean apply(List<JavaClass> input) {
                for (JavaClass parameterType : input) {
                    if (!parameterType.getModifiers().contains(PUBLIC)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private static ArchCondition<? super JavaCodeUnit> haveContravariantPredicateParameterTypes() {
        return new ArchCondition<JavaCodeUnit>("have contravariant predicate parameter types") {
            @Override
            public void check(JavaCodeUnit javaCodeUnit, ConditionEvents events) {
                Type[] parameterTypes = javaCodeUnit.isConstructor()
                        ? ((JavaConstructor) javaCodeUnit).reflect().getGenericParameterTypes()
                        : ((JavaMethod) javaCodeUnit).reflect().getGenericParameterTypes();

                stream(parameterTypes)
                        .filter(type -> type instanceof ParameterizedType)
                        .map(type -> (ParameterizedType) type)
                        .filter(type -> type.getRawType().equals(DescribedPredicate.class))
                        .map(predicateType -> predicateType.getActualTypeArguments()[0])
                        .filter(predicateTypeParameter -> !(predicateTypeParameter instanceof WildcardType)
                                || ((WildcardType) predicateTypeParameter).getLowerBounds().length == 0)
                        .forEach(type -> {
                            String message = String.format("%s has a parameter %s<%s> instead of a contravariant type parameter in %s",
                                    javaCodeUnit.getDescription(), DescribedPredicate.class.getSimpleName(), type.getTypeName(), javaCodeUnit.getSourceCodeLocation());
                            events.add(violated(javaCodeUnit, message));
                        });
            }
        };
    }

    private static DescribedPredicate<JavaClass> guavaClass() {
        return JavaClass.Functions.GET_PACKAGE_NAME.is(
                new DescribedPredicate<String>("") {
                    @Override
                    public boolean apply(String input) {
                        return input.contains(".google.");
                    }
                }).as("Guava Class");
    }

    private static DescribedPredicate<List<JavaClass>> withoutGuava() {
        return new DescribedPredicate<List<JavaClass>>("without Guava") {
            @Override
            public boolean apply(List<JavaClass> input) {
                for (JavaClass parameterType : input) {
                    if (guavaClass().apply(parameterType)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
}
