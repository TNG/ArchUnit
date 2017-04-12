package com.tngtech.archunit.library.dependencies;

import java.util.List;

import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxSeed;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxTestBase;
import com.tngtech.java.junit.dataprovider.DataProvider;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

public class RandomSlicesSyntaxTest extends RandomSyntaxTestBase {
    @DataProvider
    public static List<List<?>> random_rules() {
        return RandomSyntaxTestBase.createRandomRules(givenClassesSeed(), "^naming slices.*");
    }

    private static RandomSyntaxSeed<GivenSlices> givenClassesSeed() {
        return new RandomSyntaxSeed<>(
                GivenSlices.class,
                slices().matching("com.tngtech.archunit.(*).."),
                "slices matching 'com.tngtech.archunit.(*)..'");
    }
}
