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
    private final Supplier<Optional<Object>> annotationDefaultValue;

    private JavaMethod(Builder builder) {
        super(builder);
        methodSupplier = Suppliers.memoize(new ReflectMethodSupplier());
        annotationDefaultValue = builder.getAnnotationDefaultValue();
    }

    /**
     * Returns the default value of this annotation method, if the method is an annotation method and has a
     * declared default. It's analogue to {@link Method#getDefaultValue()}, but returns Optional.absent()
     * instead of null.
     *
     * @return Optional.of(defaultValue) if applicable, otherwise Optional.absent()
     */
    public Optional<Object> getDefaultValue() {
        return annotationDefaultValue.get();
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

    static class Builder extends JavaCodeUnit.Builder<JavaMethod, Builder> {
        private Optional<JavaAnnotation.ValueBuilder> annotationDefaultValueBuilder = Optional.absent();
        private Supplier<Optional<Object>> annotationDefaultValue = Suppliers.ofInstance(Optional.absent());

        Builder withAnnotationDefaultValue(JavaAnnotation.ValueBuilder defaultValue) {
            annotationDefaultValueBuilder = Optional.of(defaultValue);
            return this;
        }

        Supplier<Optional<Object>> getAnnotationDefaultValue() {
            return annotationDefaultValue;
        }

        @Override
        JavaMethod construct(Builder builder, final ImportedClasses.ByTypeName importedClasses) {
            if (annotationDefaultValueBuilder.isPresent()) {
                annotationDefaultValue = Suppliers.memoize(new Supplier<Optional<Object>>() {
                    @Override
                    public Optional<Object> get() {
                        return annotationDefaultValueBuilder.get().build(importedClasses);
                    }
                });
            }
            return new JavaMethod(builder);
        }
    }
}
