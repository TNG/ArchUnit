package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.ClassViolatingThirdPartyRules;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.lang.ArchCondition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.DescribedPredicate.not;
import static com.tngtech.archunit.core.JavaClass.assignableTo;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.originClassIs;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.originClassIsNot;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetIs;

public class ThirdPartyRulesTest {
    protected static final String THIRD_PARTY_CLASS_RULE_TEXT =
            "not instantiate " +
                    ThirdPartyClassWithProblem.class.getSimpleName() +
                    " and its subclasses, but instead use " +
                    ThirdPartyClassWorkaroundFactory.class.getSimpleName();

    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingThirdPartyRules.class);
    }

    @Ignore
    @Test
    public void third_party_class_should_only_be_instantiated_via_workaround() {
        all(classes).should(notCreateProblematicClassesOutsideOfWorkaroundFactory().as(THIRD_PARTY_CLASS_RULE_TEXT));
    }

    private ArchCondition<JavaClass> notCreateProblematicClassesOutsideOfWorkaroundFactory() {
        DescribedPredicate<JavaCall<?>> constructorCallOfThirdPartyClass =
                targetIs(assignableTo(ThirdPartyClassWithProblem.class), CONSTRUCTOR_NAME);

        DescribedPredicate<JavaCall<?>> notFromWithinThirdPartyClass =
                originClassIs(not(assignableTo(ThirdPartyClassWithProblem.class)));

        DescribedPredicate<JavaCall<?>> notFromWorkaroundFactory =
                originClassIsNot(ThirdPartyClassWorkaroundFactory.class);

        DescribedPredicate<JavaCall<?>> targetIsIllegalConstructorOfThirdPartyClass =
                constructorCallOfThirdPartyClass.
                        and(notFromWithinThirdPartyClass).
                        and(notFromWorkaroundFactory);

        return never(callMethodWhere(targetIsIllegalConstructorOfThirdPartyClass));
    }
}
