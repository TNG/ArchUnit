package com.tngtech.archunit.core;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.core.ArchUnitException.InconsistentClassPathException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.Formatters.formatMethod;

public class JavaMethod extends JavaCodeUnit {
    private final Supplier<Method> methodSupplier;
    private Supplier<Set<JavaMethodCall>> callsToSelf = Suppliers.ofInstance(Collections.<JavaMethodCall>emptySet());

    private JavaMethod(Builder builder) {
        super(builder);
        methodSupplier = Suppliers.memoize(new ReflectMethodSupplier());
    }

    public Set<JavaMethodCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    public Set<JavaMethodCall> getAccessesToSelf() {
        return callsToSelf.get();
    }

    @Override
    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
    public Method reflect() {
        return methodSupplier.get();
    }

    void registerCallsToMethod(Supplier<Set<JavaMethodCall>> calls) {
        this.callsToSelf = checkNotNull(calls);
    }

    static class Builder extends JavaCodeUnit.Builder<JavaMethod, Builder> {
        @Override
        JavaMethod construct(Builder builder) {
            return new JavaMethod(builder);
        }
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution procecss")
    private class ReflectMethodSupplier implements Supplier<Method> {
        @Override
        public Method get() {
            Class<?> reflectedOwner = getOwner().reflect();
            try {
                return reflectedOwner.getDeclaredMethod(getName(), reflect(getParameters()));
            } catch (NoSuchMethodException e) {
                throw new InconsistentClassPathException(
                        "Can't resolve method " + formatMethod(reflectedOwner.getName(), getName(), getParameters()), e);
            }
        }
    }
}
