package com.tngtech.archunit.lang.syntax;

import java.util.List;

import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxSeed;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxTestBase;
import com.tngtech.java.junit.dataprovider.DataProvider;

public class RandomClassesSyntaxTest extends RandomSyntaxTestBase {
    @DataProvider
    public static List<List<?>> random_rules() {
        return RandomSyntaxTestBase.createRandomRules(givenClassesSeed(),
                new SingleStringReplacement("meta annotated", "meta-annotated"));
    }

    private static RandomSyntaxSeed<GivenClasses> givenClassesSeed() {
        if (random.nextBoolean()) {
            return new RandomSyntaxSeed<>(GivenClasses.class, ArchRuleDefinition.classes(), "classes");
        } else {
            return new RandomSyntaxSeed<>(GivenClasses.class, ArchRuleDefinition.noClasses(), "no classes");
        }
    }
}
