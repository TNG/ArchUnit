package com.tngtech.archunit.lang.syntax;

import java.util.stream.Stream;

import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnits;
import com.tngtech.archunit.lang.syntax.elements.GivenConstructors;
import com.tngtech.archunit.lang.syntax.elements.GivenFields;
import com.tngtech.archunit.lang.syntax.elements.GivenMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenMethods;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxSeed;
import com.tngtech.archunit.testutil.syntax.RandomSyntaxTestBase;
import org.junit.jupiter.params.provider.Arguments;

import static java.util.function.Function.identity;

public class RandomMembersSyntaxTest extends RandomSyntaxTestBase {
    static Stream<Arguments> random_rules() {
        return Stream.of(
                createRandomMemberRules(givenMembersSeed()),
                createRandomMemberRules(givenFieldsSeed()),
                createRandomMemberRules(givenCodeUnitsSeed()),
                createRandomMemberRules(givenMethodsSeed()),
                createRandomMemberRules(givenConstructorsSeed())
        ).flatMap(identity());
    }

    private static Stream<Arguments> createRandomMemberRules(RandomSyntaxSeed<?> givenMembersSeed) {
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
