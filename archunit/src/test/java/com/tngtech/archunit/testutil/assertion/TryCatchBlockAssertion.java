package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.TryCatchBlock;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static org.assertj.core.api.Assertions.assertThat;

public class TryCatchBlockAssertion extends AbstractObjectAssert<TryCatchBlockAssertion, TryCatchBlock> {
    public TryCatchBlockAssertion(TryCatchBlock tryCatchBlock) {
        super(tryCatchBlock, TryCatchBlockAssertion.class);
    }

    public TryCatchBlockAssertion isDeclaredIn(JavaCodeUnit codeUnit) {
        assertThat(actual.getOwner()).as("owner of " + actual).isEqualTo(codeUnit);
        return this;
    }

    public TryCatchBlockAssertion catches(Class<? extends Throwable> throwable) {
        assertThatTypes(actual.getCaughtThrowables()).contain(throwable);
        return this;
    }
}
