package com.tngtech.archunit.core;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;

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
public abstract class JavaCodeUnit<M extends Member, T extends MemberDescription<M>> extends JavaMember<M, T> {
    private static final String FULL_NAME_TEMPLATE = "%s.%s(%s)";

    private Set<JavaFieldAccess> fieldAccesses;
    private Set<JavaMethodCall> methodCalls;
    private Set<JavaConstructorCall> constructorCalls;
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

    public Set<JavaFieldAccess> getFieldAccesses() {
        return fieldAccesses;
    }

    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        return methodCalls;
    }

    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        return constructorCalls;
    }

    public boolean isConstructor() {
        return false;
    }

    AccessCompletion.SubProcess completeFrom(ClassFileImportContext context) {
        ImmutableSet.Builder<JavaFieldAccess> fieldAccessesBuilder = ImmutableSet.builder();
        for (FieldAccessRecord record : context.getFieldAccessRecordsFor(this)) {
            fieldAccessesBuilder.add(new JavaFieldAccess(record));
        }
        fieldAccesses = fieldAccessesBuilder.build();

        ImmutableSet.Builder<JavaMethodCall> methodCallsBuilder = ImmutableSet.builder();
        for (AccessRecord<JavaMethod> record : context.getMethodCallRecordsFor(this)) {
            methodCallsBuilder.add(new JavaMethodCall(record));
        }
        methodCalls = methodCallsBuilder.build();

        ImmutableSet.Builder<JavaConstructorCall> constructorCallsBuilder = ImmutableSet.builder();
        for (AccessRecord<JavaConstructor> record : context.getConstructorCallRecordsFor(this)) {
            constructorCallsBuilder.add(new JavaConstructorCall(record));
        }
        constructorCalls = constructorCallsBuilder.build();

        return new AccessCompletion.SubProcess(this);
    }
}
