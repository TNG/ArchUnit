package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.ClassViolatingThirdPartyRules;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.lang.ArchCondition;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.constructor;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.originOwner;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.targetOwner;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callCodeUnitWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
public class ThirdPartyRulesTest {

    private final JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassViolatingThirdPartyRules.class);

    @Test
    public void third_party_class_should_only_be_instantiated_via_workaround() {
        classes().should(notCreateProblematicClassesOutsideOfWorkaroundFactory()
                .as("not instantiate %s and its subclasses, but instead use %s",
                        ThirdPartyClassWithProblem.class.getSimpleName(),
                        ThirdPartyClassWorkaroundFactory.class.getSimpleName()))
                .check(classes);
    }

    private ArchCondition<JavaClass> notCreateProblematicClassesOutsideOfWorkaroundFactory() {
        DescribedPredicate<JavaCall<?>> constructorCallOfThirdPartyClass =
                target(is(constructor())).and(targetOwner(is(assignableTo(ThirdPartyClassWithProblem.class))));

        DescribedPredicate<JavaCall<?>> notFromWithinThirdPartyClass =
                originOwner(is(not(assignableTo(ThirdPartyClassWithProblem.class)))).forSubType();

        DescribedPredicate<JavaCall<?>> notFromWorkaroundFactory =
                originOwner(is(not(equivalentTo(ThirdPartyClassWorkaroundFactory.class)))).forSubType();

        DescribedPredicate<JavaCall<?>> targetIsIllegalConstructorOfThirdPartyClass =
                constructorCallOfThirdPartyClass.
                        and(notFromWithinThirdPartyClass).
                        and(notFromWorkaroundFactory);

        return never(callCodeUnitWhere(targetIsIllegalConstructorOfThirdPartyClass));
    }
}
