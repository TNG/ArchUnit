package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;

public class CodeUnitAccessAssertion
        extends BaseAccessAssertion<CodeUnitAccessAssertion, JavaCodeUnitAccess<AccessTarget.CodeUnitAccessTarget>, AccessTarget.CodeUnitAccessTarget> {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CodeUnitAccessAssertion(JavaCodeUnitAccess<? extends AccessTarget.CodeUnitAccessTarget> call) {
        super((JavaCodeUnitAccess) call);
    }

    public CodeUnitAccessAssertion isTo(final JavaCodeUnit target) {
        return isTo(new Condition<AccessTarget.CodeUnitAccessTarget>("method " + target.getFullName()) {
            @Override
            public boolean matches(AccessTarget.CodeUnitAccessTarget codeUnitAccessTarget) {
                return codeUnitAccessTarget.getOwner().equals(target.getOwner())
                        && codeUnitAccessTarget.getName().equals(target.getName())
                        && codeUnitAccessTarget.getRawParameterTypes().equals(target.getRawParameterTypes());
            }
        });
    }

    public CodeUnitAccessAssertion isTo(final Class<?> codeUnitOwner) {
        return isTo(new Condition<AccessTarget.CodeUnitAccessTarget>() {
            @Override
            public boolean matches(AccessTarget.CodeUnitAccessTarget target) {
                return target.getOwner().isEquivalentTo(codeUnitOwner);
            }
        });
    }

    public CodeUnitAccessAssertion isTo(final String codeUnitName, final Class<?>... parameterTypes) {
        return isTo(new Condition<AccessTarget.CodeUnitAccessTarget>("code unit " + codeUnitName + "(" + formatNamesOf(parameterTypes) + ")") {
            @Override
            public boolean matches(AccessTarget.CodeUnitAccessTarget target) {
                return target.getName().equals(codeUnitName)
                        && HasName.Utils.namesOf(target.getRawParameterTypes()).equals(formatNamesOf(parameterTypes));
            }
        });
    }

    @Override
    protected CodeUnitAccessAssertion newAssertion(JavaCodeUnitAccess<AccessTarget.CodeUnitAccessTarget> reference) {
        return new CodeUnitAccessAssertion(reference);
    }
}
