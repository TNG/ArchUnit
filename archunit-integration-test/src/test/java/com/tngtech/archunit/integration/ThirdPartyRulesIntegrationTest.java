package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.ClassViolatingThirdPartyRules;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.thirdparty.ThirdPartySubClassWithProblem;
import com.tngtech.archunit.exampletest.ThirdPartyRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.ExpectedAccess.callFromMethod;

public class ThirdPartyRulesIntegrationTest extends ThirdPartyRulesTest {
    private static final String RULE_TEXT = "classes should " + THIRD_PARTY_CLASS_RULE_TEXT;

    @Rule
    public final ExpectedViolation expectedViolation = ExpectedViolation.none();

    @Test
    @Override
    public void third_party_class_should_only_be_instantiated_via_workaround() {
        expectedViolation.ofRule(RULE_TEXT)
                .by(callFromMethod(ClassViolatingThirdPartyRules.class, "illegallyInstantiateThirdPartyClass")
                        .toConstructor(ThirdPartyClassWithProblem.class)
                        .inLine(9))
                .by(callFromMethod(ClassViolatingThirdPartyRules.class, "illegallyInstantiateThirdPartySubClass")
                        .toConstructor(ThirdPartySubClassWithProblem.class)
                        .inLine(17));

        super.third_party_class_should_only_be_instantiated_via_workaround();
    }
}
