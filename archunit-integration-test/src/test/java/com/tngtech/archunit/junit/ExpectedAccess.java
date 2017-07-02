package com.tngtech.archunit.junit;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.junit.ExpectedMember.ExpectedOrigin;
import com.tngtech.archunit.junit.ExpectedMember.ExpectedTarget;

public abstract class ExpectedAccess {
    private final ExpectedOrigin origin;
    private final ExpectedTarget target;
    private final int lineNumber;

    ExpectedAccess(ExpectedOrigin origin, ExpectedTarget target, int lineNumber) {
        this.origin = origin;
        this.target = target;
        this.lineNumber = lineNumber;
    }

    String expectedMessage() {
        return target.messageFor(this);
    }

    ExpectedOrigin getOrigin() {
        return origin;
    }

    ExpectedTarget getTarget() {
        return target;
    }

    int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return expectedMessage();
    }

    public static ExpectedAccessViolationCreationProcess from(Class<?> origin, String method, Class<?>... paramTypes) {
        return new ExpectedAccessViolationCreationProcess(origin, method, paramTypes);
    }

    public static class ExpectedAccessViolationCreationProcess {
        private ExpectedOrigin origin;

        private ExpectedAccessViolationCreationProcess(Class<?> clazz, String method, Class<?>[] paramTypes) {
            origin = new ExpectedOrigin(clazz, method, paramTypes);
        }

        public ExpectedFieldAccessViolationBuilderStep1 accessing() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, JavaFieldAccess.AccessType.GET, JavaFieldAccess.AccessType.SET);
        }

        public ExpectedFieldAccessViolationBuilderStep1 getting() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, JavaFieldAccess.AccessType.GET);
        }

        public ExpectedFieldAccessViolationBuilderStep1 setting() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, JavaFieldAccess.AccessType.SET);
        }

        public ExpectedMethodCallViolationBuilder toMethod(Class<?> target, String member, Class<?>... paramTypes) {
            return new ExpectedMethodCallViolationBuilder(
                    origin, new ExpectedMember.ExpectedMethodTarget(target, member, paramTypes));
        }

        public ExpectedMethodCallViolationBuilder toConstructor(Class<?> target, Class<?>... paramTypes) {
            return new ExpectedMethodCallViolationBuilder(
                    origin, new ExpectedMember.ExpectedConstructorTarget(target, paramTypes));
        }
    }

    private abstract static class ExpectedAccessViolationBuilder {
        final ExpectedOrigin origin;
        final ExpectedTarget target;

        private ExpectedAccessViolationBuilder(ExpectedOrigin origin, ExpectedTarget target) {
            this.origin = origin;
            this.target = target;
        }
    }

    public static class ExpectedFieldAccessViolationBuilderStep1 {
        private final ExpectedOrigin origin;
        private final ImmutableSet<JavaFieldAccess.AccessType> accessType;

        private ExpectedFieldAccessViolationBuilderStep1(ExpectedOrigin origin, JavaFieldAccess.AccessType... accessType) {
            this.origin = origin;
            this.accessType = ImmutableSet.copyOf(accessType);
        }

        public ExpectedFieldAccessViolationBuilderStep2 field(Class<?> target, String member) {
            return new ExpectedFieldAccessViolationBuilderStep2(
                    origin, new ExpectedMember.ExpectedFieldTarget(target, member, accessType));
        }
    }

    public static class ExpectedFieldAccessViolationBuilderStep2 extends ExpectedAccessViolationBuilder {
        private ExpectedFieldAccessViolationBuilderStep2(ExpectedOrigin origin, ExpectedMember.ExpectedFieldTarget target) {
            super(origin, target);
        }

        public ExpectedFieldAccess inLine(int number) {
            return new ExpectedFieldAccess(origin, target, number);
        }
    }

    public static class ExpectedMethodCallViolationBuilder extends ExpectedAccessViolationBuilder {
        private ExpectedMethodCallViolationBuilder(ExpectedOrigin origin, ExpectedMember.ExpectedMethodTarget target) {
            super(origin, target);
        }

        public ExpectedMethodCall inLine(int number) {
            return new ExpectedMethodCall(origin, target, number);
        }
    }

    static class ExpectedFieldAccess extends ExpectedAccess {
        private ExpectedFieldAccess(ExpectedOrigin origin, ExpectedTarget target, int lineNumber) {
            super(origin, target, lineNumber);
        }
    }

    static class ExpectedMethodCall extends ExpectedAccess {
        private ExpectedMethodCall(ExpectedOrigin origin, ExpectedTarget target, int lineNumber) {
            super(origin, target, lineNumber);
        }
    }
}
