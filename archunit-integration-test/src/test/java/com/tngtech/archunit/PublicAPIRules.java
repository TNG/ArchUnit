package com.tngtech.archunit;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Formatters;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.ArchUnitArchitectureTest.THIRDPARTY_PACKAGE_IDENTIFIER;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;

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
                    .and(dont(inheritPublicAPI()))
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

    private static DescribedPredicate<JavaClass> anonymousClass() {
        return new DescribedPredicate<JavaClass>("anonymous class") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isAnonymous();
            }
        };
    }

    private static DescribedPredicate<JavaMember> declaredInClassIn(String packageIdentifier) {
        return declaredIn(resideInAPackage(packageIdentifier).as("class in '%s'", packageIdentifier));
    }

    // TODO: Would be a nice feature, to record the line numbers of members as well
    private static ArchCondition<JavaMember> notBePublic() {
        return new ArchCondition<JavaMember>("not be public") {
            @Override
            public void check(JavaMember item, ConditionEvents events) {
                boolean satisfied = !item.getModifiers().contains(PUBLIC);
                events.add(new SimpleConditionEvent(item, satisfied,
                        String.format("member %s.%s is %spublic in %s",
                                item.getOwner().getName(),
                                item.getName(),
                                satisfied ? "not " : "",
                                Formatters.formatLocation(item.getOwner(), 0))));
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
                .and(not(declaredIn(anonymousClass())))
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

    private static DescribedPredicate<JavaAnnotation> publicApiForInheritance() {
        return new DescribedPredicate<JavaAnnotation>("@%s(usage = %s)", PublicAPI.class.getSimpleName(), INHERITANCE) {
            @Override
            public boolean apply(JavaAnnotation input) {
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
}
