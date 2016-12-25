package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaField extends JavaMember {
    private final JavaClass type;
    private Supplier<Set<JavaFieldAccess>> accessesToSelf = Suppliers.ofInstance(Collections.<JavaFieldAccess>emptySet());

    private JavaField(Builder builder) {
        super(builder);
        type = builder.getType();
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
        JavaField construct(Builder builder) {
            return new JavaField(builder);
        }
    }
}
