package com.tngtech.archunit.testutil;

import java.util.Set;
import java.util.function.Predicate;

import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaCallQuery {
    private final Set<JavaMethodCall> calls;

    private JavaCallQuery(Set<JavaMethodCall> calls) {
        this.calls = requireNonNull(calls);
    }

    public JavaCallQuery from(JavaCodeUnit source) {
        return that(hasOrigin(source));
    }

    public JavaMethodCall inLineNumber(int lineNumber) {
        Set<JavaMethodCall> matchingCalls = that(hasLine(lineNumber)).calls;
        assertThat(matchingCalls).as("matching calls in line number " + lineNumber).isNotEmpty();
        return matchingCalls.iterator().next();
    }

    private JavaCallQuery that(Predicate<JavaMethodCall> predicate) {
        return new JavaCallQuery(calls.stream().filter(predicate).collect(toSet()));
    }

    public static JavaCallQuery methodCallTo(JavaMethod method) {
        return new JavaCallQuery(method.getCallsOfSelf());
    }

    private static Predicate<JavaMethodCall> hasLine(int lineNumber) {
        return input -> input.getLineNumber() == lineNumber;
    }

    private static Predicate<JavaMethodCall> hasOrigin(JavaCodeUnit origin) {
        return input -> origin.equals(input.getOrigin());
    }
}
