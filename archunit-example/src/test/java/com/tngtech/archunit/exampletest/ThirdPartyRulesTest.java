package com.tngtech.archunit.exampletest;

import com.google.common.base.Predicate;
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

import static com.google.common.base.Predicates.assignableFrom;
import static com.google.common.base.Predicates.not;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classCallsMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.originClassIs;
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
                targetIs(assignableFrom(ThirdPartyClassWithProblem.class), CONSTRUCTOR_NAME);

        Predicate<JavaCall<?>> notFromWithinThirdPartyClass =
                originClassIs(not(assignableFrom(ThirdPartyClassWithProblem.class)));

        Predicate<JavaCall<?>> notFromWorkaroundFactory =
                not(originClassIs(ThirdPartyClassWorkaroundFactory.class));

        FluentPredicate<JavaCall<?>> targetIsIllegalConstructorOfThirdPartyClass =
                constructorCallOfThirdPartyClass.
                        and(notFromWithinThirdPartyClass).
                        and(notFromWorkaroundFactory);

        return never(classCallsMethodWhere(targetIsIllegalConstructorOfThirdPartyClass));
    }
}
