package com.tngtech.archunit.core;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.HasOwner.IsOwnedByClass;

/**
 * Represents a unit of code containing accesses to other units of code. A unit of code can be
 * <ul>
 * <li>a method</li>
 * <li>a constructor</li>
 * <li>a static initializer</li>
 * </ul>
 * in particular every place, where Java code with behavior, like calling other methods or accessing fields, can
 * be defined.
 *
 * @param <M> The type of the {@link Member java.lang.reflect.Member} associated with this code unit
 * @param <T> The type of the description for this member; the description is an abstraction in case there are problems
 *            in determining a fitting {@link Member java.lang.reflect.Member}
 */
public abstract class JavaCodeUnit<M extends Member, T extends MemberDescription<M>> extends JavaMember<M, T>
        implements HasName.AndFullName, IsOwnedByClass, HasDescriptor {

    private static final String FULL_NAME_TEMPLATE = "%s.%s(%s)";

    private final JavaFieldAccesses fieldAccesses = new JavaFieldAccesses();
    private final JavaMethodCalls methodCalls = new JavaMethodCalls();
    private final JavaConstructorCalls constructorCalls = new JavaConstructorCalls();
    private final String formattedParameters;

    JavaCodeUnit(Builder<T, ?> builder) {
        this(builder.member, builder.owner);
    }

    JavaCodeUnit(T memberDescription, JavaClass owner) {
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

    public JavaMethodCalls getMethodCalls() {
        return methodCalls;
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
            methodCalls.add(new JavaMethodCall(record));
        }
        for (AccessRecord<JavaConstructor> record : context.getConstructorCallRecordsFor(this)) {
            constructorCalls.add(new JavaConstructorCall(record));
        }
    }
}
