package com.tngtech.archunit.testutil.assertion;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.SourceCodeLocation;
import com.tngtech.archunit.core.domain.TryCatchBlock;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class TryCatchBlockAssertion extends AbstractObjectAssert<TryCatchBlockAssertion, TryCatchBlock> {
    public TryCatchBlockAssertion(TryCatchBlock tryCatchBlock) {
        super(tryCatchBlock, TryCatchBlockAssertion.class);
    }

    public static TryCatchBlockCondition tryCatchBlock() {
        return new TryCatchBlockCondition();
    }

    public TryCatchBlockAssertion isDeclaredIn(JavaCodeUnit codeUnit) {
        assertThat(actual.getOwner()).as("owner of " + actual).isEqualTo(codeUnit);
        return this;
    }

    public TryCatchBlockAssertion catches(Class<? extends Throwable> throwable) {
        assertThatTypes(actual.getCaughtThrowables()).contain(throwable);
        return this;
    }

    public static class TryCatchBlockCondition extends Condition<TryCatchBlock> {
        private final List<String> description = newArrayList("try-catch-block");

        private Set<String> expectedCaughtThrowableNames;
        private String expectedSourceClassName;
        private Optional<Integer> expectedLineNumber = Optional.empty();
        private JavaCodeUnit expectedOwner;
        private boolean declaredInLambda = false;

        private TryCatchBlockCondition() {
        }

        @Override
        public boolean matches(TryCatchBlock tryCatchBlock) {
            as(Joiner.on(" ").join(description));

            boolean ownerMatches = tryCatchBlock.getOwner().equals(expectedOwner);
            boolean caughtThrowablesMatch = ImmutableSet.copyOf(namesOf(tryCatchBlock.getCaughtThrowables())).equals(expectedCaughtThrowableNames);

            SourceCodeLocation sourceCodeLocation = tryCatchBlock.getSourceCodeLocation();
            boolean sourceClassMatches = sourceCodeLocation.getSourceClass().getName().equals(expectedSourceClassName);
            boolean lineNumberMatches = expectedLineNumber.map(it -> sourceCodeLocation.getLineNumber() == it).orElse(true);
            boolean declaredInLambdaMatches = tryCatchBlock.isDeclaredInLambda() == declaredInLambda;

            return ownerMatches && caughtThrowablesMatch && sourceClassMatches && lineNumberMatches && declaredInLambdaMatches;
        }

        public TryCatchBlockCondition declaredIn(JavaCodeUnit owner) {
            expectedOwner = owner;
            expectedSourceClassName = expectedOwner.getOwner().getName();
            return this;
        }

        @SafeVarargs
        public final TryCatchBlockCondition catching(Class<? extends Throwable>... throwables) {
            List<String> throwableNames = formatNamesOf(throwables);
            description.add(String.format("catching [%s]", Joiner.on(", ").join(throwableNames)));
            this.expectedCaughtThrowableNames = ImmutableSet.copyOf(throwableNames);
            return this;
        }

        public TryCatchBlockCondition catchingNoThrowables() {
            return catching();
        }

        public TryCatchBlockCondition atLocation(Class<?> sourceOwner, int lineNumber) {
            description.add(String.format("at location %s:%d", sourceOwner.getName(), lineNumber));
            expectedSourceClassName = sourceOwner.getName();
            expectedLineNumber = Optional.of(lineNumber);
            return this;
        }

        public TryCatchBlockCondition declaredInLambda() {
            description.add("declared in lambda");
            declaredInLambda = true;
            return this;
        }
    }
}
