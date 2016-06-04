package com.tngtech.archunit.core;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class JavaField extends JavaMember<Field, MemberDescription.ForField> {
    private Set<JavaFieldAccess> accesses = Collections.emptySet();

    private JavaField(Builder builder) {
        super(builder.member, builder.owner);
    }

    @Override
    public String getFullName() {
        return getOwner().getName() + "." + getName();
    }

    public Class<?> getType() {
        return memberDescription.getType();
    }

    @Override
    public Set<JavaFieldAccess> getAccessesToSelf() {
        return accesses;
    }

    void registerAccesses(Collection<JavaFieldAccess> accesses) {
        this.accesses = ImmutableSet.copyOf(accesses);
    }

    static final class Builder extends JavaMember.Builder<MemberDescription.ForField, JavaField> {
        @Override
        public JavaField build(JavaClass owner) {
            this.owner = owner;
            return new JavaField(this);
        }

        public BuilderWithBuildParameter<JavaClass, JavaField> withField(Field field) {
            return withMember(new MemberDescription.ForDeterminedField(field));
        }
    }
}
