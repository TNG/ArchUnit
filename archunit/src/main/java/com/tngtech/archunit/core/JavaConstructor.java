package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.ArchUnitException.InconsistentClassPathException;

import static com.tngtech.archunit.core.Formatters.formatMethod;

public class JavaConstructor extends JavaCodeUnit {
    private final Supplier<Constructor<?>> constructorSupplier;
    private Set<JavaConstructorCall> callsToSelf = Collections.emptySet();

    public static final String CONSTRUCTOR_NAME = "<init>";

    private JavaConstructor(Builder builder) {
        super(builder);
        constructorSupplier = Suppliers.memoize(new ReflectConstructorSupplier());
    }

    @Override
    public boolean isConstructor() {
        return true;
    }

    public Set<JavaConstructorCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    public Set<JavaConstructorCall> getAccessesToSelf() {
        return callsToSelf;
    }

    @Override
    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
    public Constructor<?> reflect() {
        return constructorSupplier.get();
    }

    void registerCallsToConstructor(Collection<JavaConstructorCall> calls) {
        this.callsToSelf = ImmutableSet.copyOf(calls);
    }

    static final class Builder extends JavaCodeUnit.Builder<JavaConstructor, Builder> {
        @Override
        JavaConstructor construct(Builder builder) {
            return new JavaConstructor(builder);
        }
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution procecss")
    private class ReflectConstructorSupplier implements Supplier<Constructor<?>> {
        @Override
        public Constructor<?> get() {
            Class<?> reflectedOwner = getOwner().reflect();
            try {
                return reflectedOwner.getDeclaredConstructor(reflect(getParameters()));
            } catch (NoSuchMethodException e) {
                throw new InconsistentClassPathException(
                        "Can't resolve constructor " + formatMethod(reflectedOwner.getName(), getName(), getParameters()), e);
            }
        }
    }
}
