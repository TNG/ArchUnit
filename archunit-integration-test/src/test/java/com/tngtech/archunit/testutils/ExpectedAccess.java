package com.tngtech.archunit.testutils;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.testutils.ExpectedMember.ExpectedConstructorTarget;
import com.tngtech.archunit.testutils.ExpectedMember.ExpectedFieldTarget;
import com.tngtech.archunit.testutils.ExpectedMember.ExpectedMethodTarget;
import com.tngtech.archunit.testutils.ExpectedMember.ExpectedOrigin;
import com.tngtech.archunit.testutils.ExpectedMember.ExpectedTarget;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaStaticInitializer.STATIC_INITIALIZER_NAME;

public abstract class ExpectedAccess implements ExpectedRelation {
    private final ExpectedOrigin origin;
    private final ExpectedTarget target;
    private final int lineNumber;

    ExpectedAccess(ExpectedOrigin origin, ExpectedTarget target, int lineNumber) {
        this.origin = origin;
        this.target = target;
        this.lineNumber = lineNumber;
    }

    @Override
    public void associateLines(LineAssociation association) {
        association.associateIfStringIsContained(getExpectedMessage());
    }

    private String getExpectedMessage() {
        String expectedDescription = origin.getExpectedDescription() + " " + target.getExpectedDescription();
        String expectedLocation = String.format("(%s.java:%d)", origin.getDeclaringClass().getSimpleName(), lineNumber);
        return expectedDescription + " in " + expectedLocation;
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
        return getExpectedMessage();
    }

    public static ExpectedAccessViolationCreationProcess callFromConstructor(Class<?> origin, Class<?>... paramTypes) {
        return new ExpectedAccessViolationCreationProcess("Constructor", origin, CONSTRUCTOR_NAME, paramTypes);
    }

    public static ExpectedAccessViolationCreationProcess callFromStaticInitializer(Class<?> origin) {
        return new ExpectedAccessViolationCreationProcess("Static Initializer", origin, STATIC_INITIALIZER_NAME, new Class<?>[0]);
    }

    public static ExpectedAccessViolationCreationProcess callFromMethod(Class<?> origin, String method, Class<?>... paramTypes) {
        return new ExpectedAccessViolationCreationProcess("Method", origin, method, paramTypes);
    }

    @Override
    public boolean correspondsTo(Object object) {
        if (!(object instanceof JavaAccess<?>)) {
            return false;
        }
        JavaAccess<?> access = (JavaAccess<?>) object;
        return getOrigin().matches(access.getOrigin()) &&
                getTarget().matches(access.getTarget()) &&
                (getLineNumber() == access.getLineNumber());
    }

    /**
     * Instead of matching a violation corresponding to a {@link JavaAccess}, the resulting {@link ExpectedRelation}
     * will match an equivalent {@link Dependency}.
     */
    public abstract ExpectedDependency asDependency();

    public static class ExpectedAccessViolationCreationProcess {
        private ExpectedOrigin origin;

        private ExpectedAccessViolationCreationProcess(String memberDescription, Class<?> clazz, String method, Class<?>[] paramTypes) {
            origin = new ExpectedOrigin(memberDescription, clazz, method, paramTypes);
        }

        public ExpectedFieldAccessViolationBuilderStep1 getting() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, JavaFieldAccess.AccessType.GET);
        }

        public ExpectedFieldAccessViolationBuilderStep1 setting() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, JavaFieldAccess.AccessType.SET);
        }

        public ExpectedCallViolationBuilder toMethod(Class<?> target, String member, Class<?>... paramTypes) {
            return new ExpectedCallViolationBuilder(
                    origin, new ExpectedMethodTarget(target, member, paramTypes));
        }

        public ExpectedCallViolationBuilder toConstructor(Class<?> target, Class<?>... paramTypes) {
            return new ExpectedCallViolationBuilder(origin, new ExpectedConstructorTarget(target, paramTypes));
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
                    origin, new ExpectedFieldTarget(target, member, accessType));
        }
    }

    public static class ExpectedFieldAccessViolationBuilderStep2 extends ExpectedAccessViolationBuilder {
        private ExpectedFieldAccessViolationBuilderStep2(ExpectedOrigin origin, ExpectedFieldTarget target) {
            super(origin, target);
        }

        public ExpectedFieldAccess inLine(int number) {
            return new ExpectedFieldAccess(origin, target, number);
        }
    }

    public static class ExpectedCallViolationBuilder extends ExpectedAccessViolationBuilder {
        private ExpectedCallViolationBuilder(ExpectedOrigin origin, ExpectedTarget target) {
            super(origin, target);
        }

        public ExpectedCall inLine(int number) {
            return new ExpectedCall(origin, target, number);
        }
    }

    public static class ExpectedFieldAccess extends ExpectedAccess {
        private ExpectedFieldAccess(ExpectedOrigin origin, ExpectedTarget target, int lineNumber) {
            super(origin, target, lineNumber);
        }

        @Override
        public ExpectedDependency asDependency() {
            return ExpectedDependency.accessFrom(getOrigin().getDeclaringClass())
                    .toFieldDeclaredIn(getTarget().getDeclaringClass())
                    .inLineNumber(getLineNumber());
        }
    }

    public static class ExpectedCall extends ExpectedAccess {
        private ExpectedCall(ExpectedOrigin origin, ExpectedTarget target, int lineNumber) {
            super(origin, target, lineNumber);
        }

        boolean isToConstructor() {
            return getTarget().getMemberName().equals(CONSTRUCTOR_NAME);
        }

        @Override
        public ExpectedDependency asDependency() {
            return ExpectedDependency.accessFrom(getOrigin().getDeclaringClass())
                    .toCodeUnitDeclaredIn(getTarget().getDeclaringClass())
                    .inLineNumber(getLineNumber());
        }
    }
}
