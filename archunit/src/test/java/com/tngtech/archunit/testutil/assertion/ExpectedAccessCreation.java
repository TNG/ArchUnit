package com.tngtech.archunit.testutil.assertion;

import java.util.List;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.Formatters.formatMethodSimple;
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

        public Condition<JavaAccess<?>> to(final Class<?> targetClass, final String targetName) {
            return new Condition<JavaAccess<?>>(
                    String.format("%s from %s.%s to %s.%s",
                            JavaAccess.class.getSimpleName(),
                            originClass.getName(), originCodeUnitName,
                            targetClass.getSimpleName(), targetName)) {
                @Override
                public boolean matches(JavaAccess<?> access) {
                    return access.getOriginOwner().isEquivalentTo(originClass) &&
                            access.getOrigin().getName().equals(originCodeUnitName) &&
                            access.getTargetOwner().isEquivalentTo(targetClass) &&
                            access.getTarget().getName().equals(targetName);
                }
            };
        }

        public Condition<JavaAccess<?>> toConstructor(final Class<?> targetClass, final Class<?>... paramTypes) {
            final List<String> paramTypeNames = formatNamesOf(paramTypes);
            return new Condition<JavaAccess<?>>(
                    String.format("%s from %s.%s to %s",
                            JavaAccess.class.getSimpleName(),
                            originClass.getName(), originCodeUnitName,
                            formatMethodSimple(targetClass.getSimpleName(), CONSTRUCTOR_NAME, paramTypeNames))) {
                @Override
                public boolean matches(JavaAccess<?> access) {
                    return to(targetClass, CONSTRUCTOR_NAME).matches(access) &&
                            rawParameterTypeNamesOf(access).equals(paramTypeNames);
                }

                private List<String> rawParameterTypeNamesOf(JavaAccess<?> access) {
                    AccessTarget.ConstructorCallTarget target = (AccessTarget.ConstructorCallTarget) access.getTarget();
                    return HasName.Utils.namesOf(target.getRawParameterTypes());
                }
            };
        }
    }
}
