package com.tngtech.archunit.core;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.core.ArchUnitException.InconsistentClassPathException;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaField extends JavaMember {
    private final JavaClass type;
    private final Supplier<Field> fieldSupplier;
    private Supplier<Set<JavaFieldAccess>> accessesToSelf = Suppliers.ofInstance(Collections.<JavaFieldAccess>emptySet());

    private JavaField(Builder builder) {
        super(builder);
        type = builder.getType();
        fieldSupplier = Suppliers.memoize(new ReflectFieldSupplier());
    }

    @Override
    public String getFullName() {
        return getOwner().getName() + "." + getName();
    }

    public JavaClass getType() {
        return type;
    }

    @Override
    public Set<JavaFieldAccess> getAccessesToSelf() {
        return accessesToSelf.get();
    }

    @Override
    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
    public Field reflect() {
        return fieldSupplier.get();
    }

    void registerAccessesToField(Supplier<Set<JavaFieldAccess>> accesses) {
        this.accessesToSelf = checkNotNull(accesses);
    }

    static final class Builder extends JavaMember.Builder<JavaField, Builder> {
        private Type type;

        Builder withType(Type type) {
            this.type = type;
            return self();
        }

        public JavaClass getType() {
            return get(type.getClassName());
        }

        @Override
        JavaField construct(Builder builder, ImportedClasses.ByTypeName importedClasses) {
            return new JavaField(builder);
        }
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution procecss")
    private class ReflectFieldSupplier implements Supplier<Field> {
        @Override
        public Field get() {
            Class<?> reflectedOwner = getOwner().reflect();
            try {
                return reflectedOwner.getDeclaredField(getName());
            } catch (NoSuchFieldException e) {
                throw new InconsistentClassPathException(
                        String.format("Can't resolve field %s.%s", reflectedOwner.getName(), getName()), e);
            }
        }
    }
}
