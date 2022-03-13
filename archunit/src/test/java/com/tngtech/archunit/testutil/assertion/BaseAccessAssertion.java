package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static org.assertj.core.api.Assertions.assertThat;

abstract class BaseAccessAssertion<
        SELF extends BaseAccessAssertion<SELF, ACCESS, TARGET>,
        ACCESS extends JavaAccess<TARGET>,
        TARGET extends AccessTarget> {

    ACCESS access;

    BaseAccessAssertion(ACCESS access) {
        this.access = access;
    }

    public SELF isFrom(String name, Class<?>... parameterTypes) {
        return isFrom(access.getOrigin().getOwner().getCodeUnitWithParameterTypes(name, parameterTypes));
    }

    public SELF isFrom(Class<?> originClass, String name, Class<?>... parameterTypes) {
        assertThatType(access.getOriginOwner()).matches(originClass);
        return isFrom(name, parameterTypes);
    }

    public SELF isFrom(JavaCodeUnit codeUnit) {
        assertThat(access.getOrigin()).as("Origin of access").isEqualTo(codeUnit);
        return newAssertion(access);
    }

    public SELF isTo(TARGET target) {
        assertThat(access.getTarget()).as("Target of " + access.getName()).isEqualTo(target);
        return newAssertion(access);
    }

    public SELF isTo(Condition<TARGET> target) {
        assertThat(access.getTarget()).as("Target of " + access.getName()).is(target);
        return newAssertion(access);
    }

    public void inLineNumber(int number) {
        assertThat(access.getLineNumber())
                .as("Line number of access to " + access.getName())
                .isEqualTo(number);
    }

    protected abstract SELF newAssertion(ACCESS access);
}
