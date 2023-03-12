package com.tngtech.archunit.core.domain.testobjects;

@SuppressWarnings({"unused", "ConstantConditions"})
public class ClassWithDependencyOnTypeCast {

    private static final Object OBJ = new TypeCastTarget();
    private static final TypeCastTarget TARGET = (TypeCastTarget) OBJ;

    private final TypeCastTarget obj;

    ClassWithDependencyOnTypeCast(Object o) {
        this.obj = (TypeCastTarget) o;
    }

    TypeCastTarget method(Object o) {
        return (TypeCastTarget) o;
    }
    
    public static class TypeCastTarget {
    }
}
