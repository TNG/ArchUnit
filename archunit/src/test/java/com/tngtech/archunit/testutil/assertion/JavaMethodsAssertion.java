package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaMethod;
import org.assertj.core.api.AbstractIterableAssert;

import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.parameterTypes;

public class JavaMethodsAssertion
        extends AbstractIterableAssert<JavaMethodsAssertion, Iterable<? extends JavaMethod>, JavaMethod, JavaMethodAssertion> {

    public JavaMethodsAssertion(Iterable<JavaMethod> methods) {
        super(methods, JavaMethodsAssertion.class);
    }

    @Override
    protected JavaMethodAssertion toAssert(JavaMethod value, String description) {
        return new JavaMethodAssertion(value).as(description);
    }

    public JavaMethodsAssertion contain(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (!contains(owner, name, parameterTypes)) {
            throw new AssertionError(String.format("There is no method %s contained in %s",
                    formatMethod(owner.getName(), name, namesOf(parameterTypes)), actual));
        }
        return this;
    }

    private boolean contains(Class<?> owner, String name, Class<?>[] parameterTypes) {
        for (JavaMethod method : actual) {
            if (method.getOwner().isEquivalentTo(owner) && method.getName().equals(name) && parameterTypes(parameterTypes).apply(method)) {
                return true;
            }
        }
        return false;
    }
}
