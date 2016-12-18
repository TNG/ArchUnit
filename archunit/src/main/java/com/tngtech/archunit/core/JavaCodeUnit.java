package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkNotNull;
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
 */
public abstract class JavaCodeUnit extends JavaMember implements HasParameters {
    private final TypeDetails returnType;
    private final List<TypeDetails> parameters;
    private final String fullName;

    private Set<JavaFieldAccess> fieldAccesses = Collections.emptySet();
    private Set<JavaMethodCall> methodCalls = Collections.emptySet();
    private Set<JavaConstructorCall> constructorCalls = Collections.emptySet();

    JavaCodeUnit(Builder<?, ?> builder) {
        super(builder);
        this.returnType = builder.getReturnType();
        this.parameters = builder.getParameters();
        fullName = formatMethod(getOwner().getName(), getName(), getParameters());
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public List<TypeDetails> getParameters() {
        return parameters;
    }

    public TypeDetails getReturnType() {
        return returnType;
    }

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

    AccessContext.Part completeFrom(ImportContext context) {
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

        return new AccessContext.Part(this);
    }

    abstract static class Builder<OUTPUT, SELF extends Builder<OUTPUT, SELF>> extends JavaMember.Builder<OUTPUT, SELF> {
        private Type returnType;
        private Type[] parameters;

        SELF withReturnType(Type type) {
            returnType = type;
            return self();
        }

        SELF withParameters(Type[] parameters) {
            this.parameters = parameters;
            return self();
        }

        TypeDetails getReturnType() {
            return TypeDetails.of(checkNotNull(returnType));
        }

        public List<TypeDetails> getParameters() {
            return TypeDetails.allOf(parameters);
        }
    }
}
