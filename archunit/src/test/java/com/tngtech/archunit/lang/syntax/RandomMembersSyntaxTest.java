package com.tngtech.archunit.lang.syntax;

import java.util.List;

import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnits;
import com.tngtech.archunit.lang.syntax.elements.GivenConstructors;
import com.tngtech.archunit.lang.syntax.elements.GivenFields;
import com.tngtech.archunit.lang.syntax.elements.GivenMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenMethods;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxSeed;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxTestBase;
import com.tngtech.java.junit.dataprovider.DataProvider;

public class RandomMembersSyntaxTest extends RandomSyntaxTestBase {
    @DataProvider
    public static List<List<?>> random_rules() {
        return FluentIterable
                .from(createRandomMemberRules(givenMembersSeed()))
                .append(createRandomMemberRules(givenFieldsSeed()))
                .append(createRandomMemberRules(givenCodeUnitsSeed()))
                .append(createRandomMemberRules(givenMethodsSeed()))
                .append(createRandomMemberRules(givenConstructorsSeed()))
                .toList();
    }

    private static List<List<?>> createRandomMemberRules(RandomSyntaxSeed<?> givenMembersSeed) {
        return createRandomRules(givenMembersSeed,
                new SingleStringReplacement("meta annotated", "meta-annotated"));
    }

    @SuppressWarnings("rawtypes")
    private static RandomSyntaxSeed<GivenMembers> givenMembersSeed() {
        if (random.nextBoolean()) {
            return new RandomSyntaxSeed<>(GivenMembers.class, ArchRuleDefinition.members(), "members");
        } else {
            return new RandomSyntaxSeed<>(GivenMembers.class, ArchRuleDefinition.noMembers(), "no members");
        }
    }

    private static RandomSyntaxSeed<GivenFields> givenFieldsSeed() {
        if (random.nextBoolean()) {
            return new RandomSyntaxSeed<>(GivenFields.class, ArchRuleDefinition.fields(), "fields");
        } else {
            return new RandomSyntaxSeed<>(GivenFields.class, ArchRuleDefinition.noFields(), "no fields");
        }
    }

    @SuppressWarnings("rawtypes")
    private static RandomSyntaxSeed<GivenCodeUnits> givenCodeUnitsSeed() {
        if (random.nextBoolean()) {
            return new RandomSyntaxSeed<>(GivenCodeUnits.class, ArchRuleDefinition.codeUnits(), "code units");
        } else {
            return new RandomSyntaxSeed<>(GivenCodeUnits.class, ArchRuleDefinition.noCodeUnits(), "no code units");
        }
    }

    private static RandomSyntaxSeed<GivenMethods> givenMethodsSeed() {
        if (random.nextBoolean()) {
            return new RandomSyntaxSeed<>(GivenMethods.class, ArchRuleDefinition.methods(), "methods");
        } else {
            return new RandomSyntaxSeed<>(GivenMethods.class, ArchRuleDefinition.noMethods(), "no methods");
        }
    }

    private static RandomSyntaxSeed<GivenConstructors> givenConstructorsSeed() {
        if (random.nextBoolean()) {
            return new RandomSyntaxSeed<>(GivenConstructors.class, ArchRuleDefinition.constructors(), "constructors");
        } else {
            return new RandomSyntaxSeed<>(GivenConstructors.class, ArchRuleDefinition.noConstructors(), "no constructors");
        }
    }
}
