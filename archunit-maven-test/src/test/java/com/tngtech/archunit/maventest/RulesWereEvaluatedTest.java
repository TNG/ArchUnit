package com.tngtech.archunit.maventest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(PostTestVerification.class)
public class RulesWereEvaluatedTest {
    @Test
    public void rules_were_evaluated() {
        CalledRuleRecords.verifyEvaluationOf(ArchLibrary.class, ArchLibrary.RULE_ON_LEVEL_ONE_DESCRIPTOR);
        CalledRuleRecords.verifyEvaluationOf(ArchLibrary.class, ArchLibrary.RULE_METHOD_ON_LEVEL_ONE_DESCRIPTOR);
        CalledRuleRecords.verifyEvaluationOf(ArchSubLibrary.class, ArchSubLibrary.RULE_ON_LEVEL_TWO_DESCRIPTOR);
        CalledRuleRecords.verifyEvaluationOf(ArchSubLibrary.class, ArchSubLibrary.RULE_METHOD_ON_LEVEL_TWO_DESCRIPTOR);
    }
}
