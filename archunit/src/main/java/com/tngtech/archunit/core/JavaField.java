package com.tngtech.archunit.core;

import java.lang.reflect.Field;

public class JavaField extends JavaMember<Field, MemberDescription.ForField> {
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
