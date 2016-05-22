package com.tngtech.archunit.core;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.HasOwner.IsOwnedByClass;

public abstract class JavaMethodLike<M extends Member, T extends MemberDescription<M>> extends JavaMember<M, T>
        implements HasName.AndFullName, IsOwnedByClass, HasDescriptor {

    private static final String FULL_NAME_TEMPLATE = "%s.%s(%s)";

    private final JavaFieldAccesses fieldAccesses = new JavaFieldAccesses();
    private final JavaMethodCalls properMethodCalls = new JavaMethodCalls();
    private final JavaConstructorCalls constructorCalls = new JavaConstructorCalls();
    private final String formattedParameters;

    JavaMethodLike(Builder<T, ?> builder) {
        this(builder.member, builder.owner);
    }

    JavaMethodLike(T memberDescription, JavaClass owner) {
        super(memberDescription, owner);
        List<String> formatted = new ArrayList<>();
        for (Class<?> type : getParameters()) {
            formatted.add(String.format("%s.class", type.getSimpleName()));
        }
        formattedParameters = Joiner.on(", ").join(formatted);
    }

    @Override
    public String getFullName() {
        return String.format(FULL_NAME_TEMPLATE, getOwner().getName(), getName(), formattedParameters);
    }

    public abstract List<Class<?>> getParameters();

    public abstract Class<?> getReturnType();

    public JavaFieldAccesses getFieldAccesses() {
        return fieldAccesses;
    }

    public JavaMethodCalls getProperMethodCalls() {
        return properMethodCalls;
    }

    public JavaConstructorCalls getConstructorCalls() {
        return constructorCalls;
    }

    public boolean isConstructor() {
        return false;
    }

    void completeFrom(ClassFileImportContext context) {
        for (FieldAccessRecord record : context.getFieldAccessRecordsFor(this)) {
            fieldAccesses.add(new JavaFieldAccess(record));
        }
        for (AccessRecord<JavaMethod> record : context.getMethodCallRecordsFor(this)) {
            properMethodCalls.add(new JavaMethodCall(record));
        }
        for (AccessRecord<JavaConstructor> record : context.getConstructorCallRecordsFor(this)) {
            constructorCalls.add(new JavaConstructorCall(record));
        }
    }
}
