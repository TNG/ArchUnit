package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.FluentPredicate;
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

import static com.tngtech.archunit.core.FluentPredicate.not;
import static com.tngtech.archunit.core.JavaClass.assignableTo;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classCallsMethodWhere;
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
        all(classes).should(THIRD_PARTY_CLASS_RULE_TEXT).assertedBy(noCreationOutsideOfWorkaroundFactory());
    }

    private ArchCondition<JavaClass> noCreationOutsideOfWorkaroundFactory() {
        FluentPredicate<JavaCall<?>> constructorCallOfThirdPartyClass =
                targetIs(assignableTo(ThirdPartyClassWithProblem.class), CONSTRUCTOR_NAME);

        FluentPredicate<JavaCall<?>> notFromWithinThirdPartyClass =
                originClassIs(not(assignableTo(ThirdPartyClassWithProblem.class)));

        FluentPredicate<JavaCall<?>> notFromWorkaroundFactory =
                originClassIsNot(ThirdPartyClassWorkaroundFactory.class);

        FluentPredicate<JavaCall<?>> targetIsIllegalConstructorOfThirdPartyClass =
                constructorCallOfThirdPartyClass.
                        and(notFromWithinThirdPartyClass).
                        and(notFromWorkaroundFactory);

        return never(classCallsMethodWhere(targetIsIllegalConstructorOfThirdPartyClass));
    }
}
