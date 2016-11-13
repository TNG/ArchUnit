package com.tngtech.archunit.core;

import java.util.List;
import java.util.Objects;

import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;

public abstract class AccessTarget implements HasName.AndFullName, HasOwner<JavaClass> {
    private final String name;
    private final JavaClass owner;

    AccessTarget(JavaClass owner, String name) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JavaClass getOwner() {
        return owner;
    }

    public static class FieldAccessTarget extends AccessTarget {
        private final TypeDetails type;
        private final JavaField field;
        private final String fullName;

        // FIXME: Solve access target vs actual member
        FieldAccessTarget(JavaField field) {
            this(field.getOwner(), field.getName(), TypeDetails.of(field.getType()), field);
        }

        // FIXME: Solve access target vs actual member
        FieldAccessTarget(JavaClass owner, String name, TypeDetails type, JavaField field) {
            super(owner, name);
            this.type = type;
            this.field = field;
            fullName = owner.getName() + "." + name;
        }

        @Override
        public String getFullName() {
            return fullName;
        }

        public TypeDetails getType() {
            return type;
        }

        public JavaField getJavaField() {
            return field;
        }

        @Override
        public int hashCode() {
            return Objects.hash(field);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final FieldAccessTarget other = (FieldAccessTarget) obj;
            return Objects.equals(this.field, other.field);
        }
    }

    public static class CodeUnitCallTarget extends AccessTarget implements HasParameters {
        private final List<TypeDetails> parameters;
        private final String fullName;

        CodeUnitCallTarget(JavaClass owner, String name, List<TypeDetails> parameters) {
            super(owner, name);
            this.parameters = parameters;
            fullName = Formatters.formatMethod(owner.getName(), name, parameters);
        }

        @Override
        public String getFullName() {
            return fullName;
        }

        @Override
        public List<TypeDetails> getParameters() {
            return parameters;
        }
    }

    public static class ConstructorCallTarget extends CodeUnitCallTarget {
        private final JavaConstructor constructor;

        // FIXME: Solve access target vs actual member
        ConstructorCallTarget(JavaConstructor constructor) {
            this(constructor.getOwner(), constructor.getParameters(), constructor);
        }

        // FIXME: Solve access target vs actual member
        ConstructorCallTarget(JavaClass owner, List<TypeDetails> parameters, JavaConstructor constructor) {
            super(owner, CONSTRUCTOR_NAME, parameters);
            this.constructor = constructor;
        }

        public JavaConstructor getConstructor() {
            return constructor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(constructor);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ConstructorCallTarget other = (ConstructorCallTarget) obj;
            return Objects.equals(this.constructor, other.constructor);
        }
    }

    public static class MethodCallTarget extends CodeUnitCallTarget {
        private final TypeDetails returnType;
        private final JavaMethod method;

        // FIXME: Solve access target vs actual member
        MethodCallTarget(JavaMethod method) {
            this(method.getOwner(), method.getName(), method.getParameters(), method.getReturnType(), method);
        }

        // FIXME: Solve access target vs actual member
        MethodCallTarget(JavaClass owner, String name, List<TypeDetails> parameters, TypeDetails returnType, JavaMethod method) {
            super(owner, name, parameters);
            this.returnType = returnType;
            this.method = method;
        }

        public JavaMethod getMethod() {
            return method;
        }

        public TypeDetails getReturnType() {
            return returnType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(method);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MethodCallTarget other = (MethodCallTarget) obj;
            return Objects.equals(this.method, other.method);
        }
    }
}
