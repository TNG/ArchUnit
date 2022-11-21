package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.TryCatchBlock;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static org.assertj.core.api.Assertions.assertThat;

public class CodeUnitAccessAssertion
        extends BaseAccessAssertion<CodeUnitAccessAssertion, JavaCodeUnitAccess<AccessTarget.CodeUnitAccessTarget>, AccessTarget.CodeUnitAccessTarget> {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CodeUnitAccessAssertion(JavaCodeUnitAccess<? extends AccessTarget.CodeUnitAccessTarget> call) {
        super((JavaCodeUnitAccess) call);
    }

    public CodeUnitAccessAssertion isTo(JavaCodeUnit target) {
        return isTo(new Condition<AccessTarget.CodeUnitAccessTarget>("method " + target.getFullName()) {
            @Override
            public boolean matches(AccessTarget.CodeUnitAccessTarget codeUnitAccessTarget) {
                return codeUnitAccessTarget.getOwner().equals(target.getOwner())
                        && codeUnitAccessTarget.getName().equals(target.getName())
                        && codeUnitAccessTarget.getRawParameterTypes().equals(target.getRawParameterTypes());
            }
        });
    }

    public CodeUnitAccessAssertion isTo(Class<?> codeUnitOwner) {
        return isTo(new Condition<AccessTarget.CodeUnitAccessTarget>() {
            @Override
            public boolean matches(AccessTarget.CodeUnitAccessTarget target) {
                return target.getOwner().isEquivalentTo(codeUnitOwner);
            }
        });
    }

    public CodeUnitAccessAssertion isTo(String codeUnitName, Class<?>... parameterTypes) {
        return isTo(new Condition<AccessTarget.CodeUnitAccessTarget>("code unit " + codeUnitName + "(" + formatNamesOf(parameterTypes) + ")") {
            @Override
            public boolean matches(AccessTarget.CodeUnitAccessTarget target) {
                return target.getName().equals(codeUnitName)
                        && HasName.Utils.namesOf(target.getRawParameterTypes()).equals(formatNamesOf(parameterTypes));
            }
        });
    }

    public CodeUnitAccessAssertion isTo(Class<?> targetClass, String codeUnitName, Class<?>... parameterTypes) {
        return isTo(targetClass).isTo(codeUnitName, parameterTypes);
    }

    public CodeUnitAccessAssertion isWrappedWithTryCatchFor(Class<? extends Throwable> throwableType) {
        assertThat(access.getContainingTryBlocks())
                .as("containing try blocks")
                .areExactly(1, caughtThrowableOfType(throwableType));
        return this;
    }

    public CodeUnitAccessAssertion isNotWrappedInTryCatch() {
        assertThat(access.getContainingTryBlocks())
                .as("not wrapped with try/catch")
                .isEmpty();
        return this;
    }

    private Condition<TryCatchBlock> caughtThrowableOfType(Class<? extends Throwable> expectedThrowable) {
        return new Condition<TryCatchBlock>("caught throwable of type " + expectedThrowable.getSimpleName()) {
            @Override
            public boolean matches(TryCatchBlock tryCatchBlock) {
                return tryCatchBlock.getCaughtThrowables().stream()
                        .anyMatch(throwable -> throwable.isEquivalentTo(expectedThrowable));
            }
        };
    }

    @Override
    protected CodeUnitAccessAssertion newAssertion(JavaCodeUnitAccess<AccessTarget.CodeUnitAccessTarget> reference) {
        return new CodeUnitAccessAssertion(reference);
    }
}
