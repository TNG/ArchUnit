package com.tngtech.archunit.core;

import java.lang.reflect.Member;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;

import static com.tngtech.archunit.core.Formatters.formatMethod;

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
public abstract class JavaCodeUnit<M extends Member, T extends MemberDescription<M>>
        extends JavaMember<M, T>
        implements HasParameters {

    private Set<JavaFieldAccess> fieldAccesses;
    private Set<JavaMethodCall> methodCalls;
    private Set<JavaConstructorCall> constructorCalls;
    private String fullName;

    JavaCodeUnit(Builder<T, ?> builder) {
        this(builder.member, builder.owner);
    }

    JavaCodeUnit(T memberDescription, JavaClass owner) {
        super(memberDescription, owner);
        fullName = formatMethod(getOwner().getName(), getName(), getParameters());
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    public abstract TypeDetails getReturnType();

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
        for (AccessRecord<MethodCallTarget> record : context.getMethodCallRecordsFor(this)) {
            methodCallsBuilder.add(new JavaMethodCall(record));
        }
        methodCalls = methodCallsBuilder.build();

        ImmutableSet.Builder<JavaConstructorCall> constructorCallsBuilder = ImmutableSet.builder();
        for (AccessRecord<ConstructorCallTarget> record : context.getConstructorCallRecordsFor(this)) {
            constructorCallsBuilder.add(new JavaConstructorCall(record));
        }
        constructorCalls = constructorCallsBuilder.build();

        return new AccessCompletion.SubProcess(this);
    }
}
