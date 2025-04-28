package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import com.tngtech.archunit.testutil.syntax.MethodChoiceStrategy;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxSeed;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxTestBase;
import org.junit.jupiter.params.provider.Arguments;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

public class RandomSlicesSyntaxTest extends RandomSyntaxTestBase {
    static Stream<Arguments> random_rules() {
        return RandomSyntaxTestBase.createRandomRules(givenClassesSeed(),
                MethodChoiceStrategy.chooseAllArchUnitSyntaxMethods().exceptMethodsWithName("ignoreDependency"),
                new Skip("^naming slices.*"),
                new ReplaceEverythingSoFar("as '([^']+)'", "$1"));
    }

    private static RandomSyntaxSeed<GivenSlices> givenClassesSeed() {
        return new RandomSyntaxSeed<>(
                GivenSlices.class,
                slices().matching("com.tngtech.archunit.(*).."),
                "slices matching 'com.tngtech.archunit.(*)..'");
    }

    private static class Skip implements DescriptionReplacement {
        private final Pattern skipPattern;

        Skip(String pattern) {
            skipPattern = Pattern.compile(pattern);
        }

        @Override
        public boolean applyTo(String currentToken, List<String> currentDescription) {
            return skipPattern.matcher(currentToken).matches();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + skipPattern + "}";
        }
    }
}
