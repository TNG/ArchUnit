package com.tngtech.archunit.testutil.assertion;

import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.Formatters.formatMethodParameterTypeNames;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;

public class ExpectedAccessCreation {
    public ExpectedAccessCreation() {
    }

    public Step2 from(Class<?> originClass, String codeUnitName) {
        return new Step2(originClass, codeUnitName);
    }

    public static class Step2 {
        private final Class<?> originClass;
        private final String originCodeUnitName;

        private Step2(Class<?> originClass, String originCodeUnitName) {
            this.originClass = originClass;
            this.originCodeUnitName = originCodeUnitName;
        }

        public ExpectedAccessCondition to(final Class<?> targetClass, final String targetName) {
            return new ExpectedAccessCondition(originClass, originCodeUnitName, targetClass, targetName);
        }

        public ExpectedFieldAccessCondition toField(AccessType accessType, final Class<?> targetClass, final String targetName) {
            return new ExpectedFieldAccessCondition(originClass, originCodeUnitName, accessType, targetClass, targetName);
        }

        public ExpectedConstructorCallCondition toConstructor(final Class<?> targetClass, final Class<?>... paramTypes) {
            return new ExpectedConstructorCallCondition(originClass, originCodeUnitName, targetClass, formatNamesOf(paramTypes));
        }
    }

    public static class ExpectedAccessCondition extends Condition<JavaAccess<?>> {
        private final Class<?> originClass;
        private final String originCodeUnitName;
        private final Class<?> targetClass;
        private final String targetName;
        private Optional<Boolean> declaredInLambda = Optional.empty();

        private ExpectedAccessCondition(Class<?> originClass, String originCodeUnitName, Class<?> targetClass, String targetName) {
            super(
                    String.format("%s from %s.%s to %s.%s",
                            JavaAccess.class.getSimpleName(),
                            originClass.getName(), originCodeUnitName,
                            targetClass.getSimpleName(), targetName)
            );
            this.originClass = originClass;
            this.originCodeUnitName = originCodeUnitName;
            this.targetClass = targetClass;
            this.targetName = targetName;
        }

        @Override
        public boolean matches(JavaAccess<?> access) {
            return access.getOriginOwner().isEquivalentTo(originClass) &&
                    access.getOrigin().getName().equals(originCodeUnitName) &&
                    access.getTargetOwner().isEquivalentTo(targetClass) &&
                    access.getTarget().getName().equals(targetName) &&
                    declaredInLambda.map(expected -> access.isDeclaredInLambda() == expected).orElse(true);
        }

        public ExpectedAccessCondition declaredInLambda() {
            this.declaredInLambda = Optional.of(true);
            return this;
        }
    }

    public static class ExpectedFieldAccessCondition extends ExpectedAccessCondition {
        private final AccessType accessType;

        private ExpectedFieldAccessCondition(Class<?> originClass, String originCodeUnitName, AccessType accessType, Class<?> targetClass, String targetName) {
            super(originClass, originCodeUnitName, targetClass, targetName);
            this.accessType = accessType;
            as(String.format("%s (accessType: %s)", description(), accessType));
        }

        @Override
        public boolean matches(JavaAccess<?> access) {
            return accessTypeMatches(access, accessType) && super.matches(access);
        }

        private boolean accessTypeMatches(JavaAccess<?> access, AccessType accessType) {
            return access instanceof JavaFieldAccess && ((JavaFieldAccess) access).getAccessType().equals(accessType);
        }
    }

    public static class ExpectedConstructorCallCondition extends ExpectedAccessCondition {
        private final List<String> paramTypeNames;

        private ExpectedConstructorCallCondition(Class<?> originClass, String originCodeUnitName, Class<?> targetClass, List<String> paramTypeNames) {
            super(originClass, originCodeUnitName, targetClass, CONSTRUCTOR_NAME);
            this.paramTypeNames = paramTypeNames;
            as(description() + "(" + formatMethodParameterTypeNames(paramTypeNames) + ")");
        }

        @Override
        public boolean matches(JavaAccess<?> access) {
            return super.matches(access) && rawParameterTypeNamesOf(access).equals(paramTypeNames);
        }

        private List<String> rawParameterTypeNamesOf(JavaAccess<?> access) {
            AccessTarget.ConstructorCallTarget target = (AccessTarget.ConstructorCallTarget) access.getTarget();
            return HasName.Utils.namesOf(target.getRawParameterTypes());
        }
    }
}
