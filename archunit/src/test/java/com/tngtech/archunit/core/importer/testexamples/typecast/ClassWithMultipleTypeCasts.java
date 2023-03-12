package com.tngtech.archunit.core.importer.testexamples.typecast;

import java.util.List;


@SuppressWarnings({"StatementWithEmptyBody", "unused", "ConstantConditions"})
public class ClassWithMultipleTypeCasts {
    private static final Object OBJ = new TypeCastTestedType();
    private static final TypeCastTestedType TARGET = (TypeCastTestedType) OBJ;

    private final TypeCastTestedType obj;

    ClassWithMultipleTypeCasts(Object o) {
        this.obj = (TypeCastTestedType) o;
    }

    TypeCastTestedType methodWithoutCast(TypeCastTestedType o) {
        return o;
    }

    TypeCastTestedType methodWithoutCast(Object o) {
        return TypeCaster.cast(o);
    }

    TypeCastTestedType methodWithExplicitCast(Object o) {
        return (TypeCastTestedType) o;
    }

    TypeCastTestedType methodWithImplicitCastUsingClassCast(Object o) {
        return TypeCastTestedType.class.cast(o);
    }

    TypeCastTestedType methodWithExplicitCastInLambdas(Object o) {
        List<Object> objects = List.of(new TypeCastTestedType());

        return objects.stream().map(obj -> (TypeCastTestedType) obj)
                .findFirst()
                .get();
    }

    TypeCastTestedType methodWithImplicitCastInLambdas(Object o) {
        List<Object> objects = List.of(new TypeCastTestedType());

        return objects.stream().map(TypeCastTestedType.class::cast)
                .findFirst()
                .get();
    }

    TypeCastTestedType methodWithImplicitCastDueToDelegation(Object o) {
        TypeCaster<TypeCastTestedType> caster = new TypeCaster<>(o, TypeCastTestedType.class);
        TypeCastTestedType result = caster.cast();
        System.out.println(result);

        return result;
    }
    
    private static class TypeCaster<T> {
        private T target;

        @SuppressWarnings("unchecked")
        TypeCaster(Object target, Class<T> type) {
            this.target = (T) target;
        }

        public T cast() {
            return target;
        }

        static TypeCastTestedType cast(Object o) {
            return (TypeCastTestedType) o;
        }
    }
}
