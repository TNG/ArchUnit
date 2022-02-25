package com.tngtech.archunit.testutil.assertion;

import java.util.List;
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
import static org.assertj.core.util.Lists.newArrayList;

public class TryCatchBlocksAssertion extends AbstractObjectAssert<TryCatchBlocksAssertion, Set<TryCatchBlock>> {
    public TryCatchBlocksAssertion(Set<TryCatchBlock> tryCatchBlocks) {
        super(tryCatchBlocks, TryCatchBlocksAssertion.class);
    }

    public static TryCatchBlockCondition tryCatchBlock() {
        return new TryCatchBlockCondition();
    }

    public static class TryCatchBlockCondition extends Condition<TryCatchBlock> {
        private final List<String> description = newArrayList("try-catch-block");

        private Set<String> expectedCaughtThrowableNames;
        private Class<?> expectedSourceClass;
        private int expectedLineNumber;
        private JavaCodeUnit expectedOwner;

        private TryCatchBlockCondition() {
        }

        @Override
        public boolean matches(TryCatchBlock tryCatchBlock) {
            as(Joiner.on(" ").join(description));

            boolean ownerMatches = tryCatchBlock.getOwner().equals(expectedOwner);
            boolean caughtThrowablesMatch = ImmutableSet.copyOf(namesOf(tryCatchBlock.getCaughtThrowables())).equals(expectedCaughtThrowableNames);

            SourceCodeLocation sourceCodeLocation = tryCatchBlock.getSourceCodeLocation();
            boolean sourceClassMatches = sourceCodeLocation.getSourceClass().isEquivalentTo(expectedSourceClass);
            boolean lineNumberMatches = sourceCodeLocation.getLineNumber() == expectedLineNumber;

            return ownerMatches && caughtThrowablesMatch && sourceClassMatches && lineNumberMatches;
        }

        public TryCatchBlockCondition declaredIn(JavaCodeUnit owner) {
            expectedOwner = owner;
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
            expectedSourceClass = sourceOwner;
            expectedLineNumber = lineNumber;
            return this;
        }
    }
}
