package com.tngtech.archunit.testutil.syntax;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import static com.google.common.base.Preconditions.checkState;

class PropagatedType {
    private final Type type;
    private final Map<String, Type> typeVariables;

    PropagatedType(Type type) {
        this(type, Context.empty());
    }

    private PropagatedType(Type type, Context context) {
        this.type = type;
        typeVariables = getTypeVariables(type, context);
    }

    private Map<String, Type> getTypeVariables(Type type, Context context) {
        if (type instanceof Class<?>) {
            return resolveTypeVariables((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            return resolveTypeVariables((ParameterizedType) type, context);
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<String, Type> resolveTypeVariables(Class<?> clazz) {
        ImmutableMap.Builder<String, Type> result = ImmutableMap.builder();
        for (TypeVariable<? extends Class<?>> typeParameter : clazz.getTypeParameters()) {
            result.put(typeParameter.getName(), getOnlyBound(typeParameter));
        }
        return result.build();
    }

    private Type getOnlyBound(TypeVariable<? extends Class<?>> typeParameter) {
        checkState(typeParameter.getBounds().length == 1,
                "Can't deal with multiple bounds yet -> %s<%s>", typeParameter.getGenericDeclaration(), typeParameter);
        return typeParameter.getBounds()[0];
    }

    private Map<String, Type> resolveTypeVariables(ParameterizedType parameterizedType, Context context) {
        TypeVariable<? extends Class<?>>[] typeParameters = ((Class<?>) parameterizedType.getRawType()).getTypeParameters();
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        checkState(typeParameters.length == actualTypeArguments.length,
                "Number of type parameters %d and actual type arguments %d do not match for %s",
                typeParameters.length, actualTypeArguments.length, parameterizedType);

        ImmutableMap.Builder<String, Type> result = ImmutableMap.builder();
        for (int i = 0; i < typeParameters.length; i++) {
            Type resolvedType = resolveMoreSpecificType(typeParameters[i], actualTypeArguments[i], context);
            result.put(typeParameters[i].getName(), resolvedType);
        }
        return result.build();
    }

    private Type resolveMoreSpecificType(TypeVariable<? extends Class<?>> typeVariable, Type actualTypeArgument, Context context) {
        Class<?> bound = TypeToken.of(getOnlyBound(typeVariable)).getRawType();
        Class<?> actual = TypeToken.of(context.resolve(actualTypeArgument)).getRawType();
        return bound.isAssignableFrom(actual) ? actual : bound;
    }

    Class<?> getRawType() {
        return TypeToken.of(type).getRawType();
    }

    PropagatedType resolveType(Type type) {
        if (type instanceof TypeVariable<?>) {
            return resolveTypeVariable((TypeVariable<?>) type);
        }
        return new PropagatedType(type, Context.forType(this));
    }

    private PropagatedType resolveTypeVariable(TypeVariable<?> typeVariable) {
        checkState(typeVariables.containsKey(typeVariable.getName()),
                "Tried to resolve unknown %s %s", TypeVariable.class.getSimpleName(), typeVariable);

        return new PropagatedType(typeVariables.get(typeVariable.getName()), Context.forType(this));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + type + ']';
    }

    private static class Context {
        private final Map<String, Type> typeVariables;

        private Context(Map<String, Type> typeVariables) {
            this.typeVariables = typeVariables;
        }

        static Context forType(PropagatedType type) {
            return new Context(type.typeVariables);
        }

        static Context empty() {
            return new Context(Collections.<String, Type>emptyMap());
        }

        Type resolve(Type type) {
            if (!(type instanceof TypeVariable<?>)) {
                return type;
            }

            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            return typeVariables.containsKey(typeVariable.getName())
                    ? typeVariables.get(typeVariable.getName())
                    : type;
        }
    }
}
