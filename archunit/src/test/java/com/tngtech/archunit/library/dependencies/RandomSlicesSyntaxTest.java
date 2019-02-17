package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import com.tngtech.archunit.testutil.syntax.MethodChoiceStrategy;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxSeed;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxTestBase;
import com.tngtech.java.junit.dataprovider.DataProvider;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

public class RandomSlicesSyntaxTest extends RandomSyntaxTestBase {
    @DataProvider
    public static List<List<?>> random_rules() {
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

    private static class ReplaceEverythingSoFar implements DescriptionReplacement {
        private final Pattern pattern;
        private final String replaceWith;

        ReplaceEverythingSoFar(String pattern, String replaceWith) {
            this.pattern = Pattern.compile(pattern);
            this.replaceWith = replaceWith;
        }

        @Override
        public boolean applyTo(String currentToken, List<String> currentDescription) {
            Matcher matcher = pattern.matcher(currentToken);
            if (matcher.matches()) {
                currentDescription.clear();
                currentDescription.add(matcher.replaceAll(replaceWith));
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{/" + pattern + "/" + replaceWith + "/}";
        }
    }
}
