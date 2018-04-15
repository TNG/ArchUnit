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

    private static class SingleStringReplacement implements DescriptionReplacement {

        private final String search;

        private final String replacement;

        private SingleStringReplacement(String search, String replacement) {
            this.search = search;
            this.replacement = replacement;
        }

        @Override
        public boolean applyTo(String currentToken, List<String> currentDescription) {
            if (currentToken.contains(search)) {
                currentDescription.add(currentToken.replace(search, replacement));
                return true;
            }

            return false;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{/" + search + "/" + replacement + "/}";
        }
    }
}
