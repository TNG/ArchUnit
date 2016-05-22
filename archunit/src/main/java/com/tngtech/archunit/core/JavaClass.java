package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.JavaClass.TypeAnalysisListener.NO_OP;
import static com.tngtech.archunit.core.Optionals.valueOrException;

public class JavaClass implements HasName {
    private final Class<?> type;
    private final Set<JavaField> fields;
    private final Set<JavaMethodLike<?, ?>> methods;
    private final Set<JavaMethod> properMethods = new HashSet<>();
    private final Set<JavaConstructor> constructors = new HashSet<>();
    private final JavaStaticInitializer staticInitializer;
    private Optional<JavaClass> superClass = Optional.absent();
    private final Set<JavaClass> subClasses = new HashSet<>();
    private Optional<JavaClass> enclosingClass = Optional.absent();

    private JavaClass(Builder builder) {
        type = checkNotNull(builder.type);
        fields = build(builder.fieldBuilders, this);
        methods = build(builder.methodBuilders, this);

        staticInitializer = addMethodsByTypeAndReturnStaticInitializer(methods);

        checkNotNull(type);
    }

    private JavaStaticInitializer addMethodsByTypeAndReturnStaticInitializer(Set<JavaMethodLike<?, ?>> methods) {
        JavaStaticInitializer result = null;
        for (JavaMethodLike<?, ?> method : methods) {
            if (method instanceof JavaMethod) {
                properMethods.add((JavaMethod) method);
            } else if (method instanceof JavaConstructor) {
                constructors.add((JavaConstructor) method);
            } else {
                result = (JavaStaticInitializer) method;
            }
        }
        return checkNotNull(result, "No static initializer found for class " + this + ". Something went wrong");
    }

    @Override
    public String getName() {
        return type.getName();
    }

    public String getSimpleName() {
        return type.getSimpleName();
    }

    public String getPackage() {
        return type.getPackage() != null ? type.getPackage().getName() : "";
    }

    public Optional<JavaClass> getSuperClass() {
        return superClass;
    }

    public Set<JavaClass> getSubClasses() {
        return subClasses;
    }

    public Optional<JavaClass> getEnclosingClass() {
        return enclosingClass;
    }

    public Set<JavaClass> getAllSubClasses() {
        Set<JavaClass> result = new HashSet<>();
        for (JavaClass subClass : subClasses) {
            result.add(subClass);
            result.addAll(subClass.getAllSubClasses());
        }
        return result;
    }

    public Set<JavaField> getFields() {
        return fields;
    }

    public JavaField getField(String name) {
        return valueOrException(tryGetField(name),
                new IllegalArgumentException("No field with name '" + name + " in class " + getName()));
    }

    public Optional<JavaField> tryGetField(String name) {
        for (JavaField field : fields) {
            if (name.equals(field.getName())) {
                return Optional.of(field);
            }
        }
        return Optional.absent();
    }

    public Set<JavaMethodLike<?, ?>> getMethods() {
        return methods;
    }

    public JavaMethodLike<?, ?> getMethod(String name, Class<?>... parameters) {
        return findMatchingMethod(methods, name, parameters);
    }

    private <T extends JavaMethodLike<?, ?>> T findMatchingMethod(Set<T> methods, String name, Class<?>[] parameters) {
        return findMatchingMethod(methods, name, newArrayList(parameters));
    }

    private <T extends JavaMethodLike<?, ?>> T findMatchingMethod(Set<T> methods, String name, List<Class<?>> parameters) {
        return valueOrException(tryFindMatchingMethod(methods, name, parameters),
                new IllegalArgumentException("No method with name '" + name + "' and parameters " + parameters +
                        " in methods " + methods + " of class " + getName()));
    }

    private <T extends JavaMethodLike<?, ?>> Optional<T> tryFindMatchingMethod(Set<T> methods, String name, List<Class<?>> parameters) {
        for (T method : methods) {
            if (name.equals(method.getName()) && parameters.equals(method.getParameters())) {
                return Optional.of(method);
            }
        }
        return Optional.absent();
    }

    /**
     * @return A proper method (represented by a Java Method) with the given signature as opposed to
     * {@link JavaMethodLike}s that represent Java Constructors
     */
    public JavaMethod getProperMethod(String name, Class<?>... parameters) {
        return findMatchingMethod(properMethods, name, parameters);
    }

    /**
     * @return Only Methods that are represented by proper Java Methods as opposed to
     * {@link JavaMethodLike}s that represent Java Constructors
     */
    public Set<JavaMethod> getProperMethods() {
        return properMethods;
    }

    /**
     * @return A proper constructor (represented by a Java Constructor) with the given signature as opposed to
     * {@link JavaMethodLike}s that represent Java Methods
     */
    public JavaConstructor getConstructor(Class<?>... parameters) {
        return findMatchingMethod(constructors, JavaConstructor.CONSTRUCTOR_NAME, parameters);
    }

    public Set<JavaConstructor> getConstructors() {
        return constructors;
    }

    public JavaStaticInitializer getStaticInitializer() {
        return staticInitializer;
    }

    public JavaFieldAccesses getFieldAccesses() {
        JavaFieldAccesses result = new JavaFieldAccesses();
        for (JavaMethodLike<?, ?> method : methods) {
            result.addAll(method.getFieldAccesses());
        }
        return result;
    }

    /**
     * Returns all calls to methods of this class, where methods refers to 'proper' methods as well as constructors.
     *
     * @see #getProperMethodCalls()
     */
    public Set<JavaMethodLikeCall<?>> getMethodCalls() {
        return Sets.<JavaMethodLikeCall<?>>union(getProperMethodCalls(), getConstructorCalls());
    }

    public JavaMethodCalls getProperMethodCalls() {
        JavaMethodCalls result = new JavaMethodCalls();
        for (JavaMethodLike<?, ?> method : methods) {
            result.addAll(method.getProperMethodCalls());
        }
        return result;
    }

    public JavaConstructorCalls getConstructorCalls() {
        JavaConstructorCalls result = new JavaConstructorCalls();
        for (JavaMethodLike<?, ?> method : methods) {
            result.addAll(method.getConstructorCalls());
        }
        return result;
    }

    public Set<Dependency> getDirectDependencies() {
        Set<Dependency> result = new HashSet<>();
        for (JavaAccess<?> access : filterTargetNotSelf(getFieldAccesses())) {
            result.add(Dependency.from(access));
        }
        for (JavaAccess<?> call : filterTargetNotSelf(getMethodCalls())) {
            result.add(Dependency.from(call));
        }
        return result;
    }

    private Set<JavaAccess<?>> filterTargetNotSelf(Set<? extends JavaAccess<?>> accesses) {
        Set<JavaAccess<?>> result = new HashSet<>();
        for (JavaAccess<?> access : accesses) {
            if (!access.getTarget().getOwner().equals(this)) {
                result.add(access);
            }
        }
        return result;
    }

    public Class<?> reflect() {
        return type;
    }

    CompletionProcess completeClassHierarchyFrom(ClassFileImportContext context) {
        superClass = findClass(type.getSuperclass(), context);
        if (superClass.isPresent()) {
            superClass.get().subClasses.add(this);
        }
        enclosingClass = findClass(type.getEnclosingClass(), context);
        return new CompletionProcess();
    }

    private static Optional<JavaClass> findClass(Class<?> clazz, ClassFileImportContext context) {
        return clazz != null ? context.tryGetJavaClassWithType(clazz) : Optional.<JavaClass>absent();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaClass other = (JavaClass) obj;
        return Objects.equals(this.type, other.type);
    }

    @Override
    public String toString() {
        return "JavaClass{name='" + type.getName() + "\'}";
    }

    public static Predicate<JavaClass> withType(final Class<?> type) {
        return new Predicate<JavaClass>() {
            @Override
            public boolean apply(JavaClass input) {
                return input.reflect().equals(type);
            }
        };
    }

    class CompletionProcess {
        void completeMethodsFrom(ClassFileImportContext context) {
            for (JavaMethodLike<?, ?> method : methods) {
                method.completeFrom(context);
            }
        }
    }

    static final class Builder {
        private Class<?> type;
        private final Set<BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuilders = new HashSet<>();
        private final Set<BuilderWithBuildParameter<JavaClass, ? extends JavaMethodLike<?, ?>>> methodBuilders = new HashSet<>();
        private final TypeAnalysisListener analysisListener;

        Builder() {
            this(NO_OP);
        }

        Builder(TypeAnalysisListener analysisListener) {
            this.analysisListener = analysisListener;
        }

        @SuppressWarnings("unchecked")
        Builder withType(Class<?> type) {
            this.type = type;
            for (Field field : type.getDeclaredFields()) {
                fieldBuilders.add(new JavaField.Builder().withField(field));
            }
            for (Method method : type.getDeclaredMethods()) {
                analysisListener.onMethodFound(method);
                methodBuilders.add(new JavaMethod.Builder().withMethod(method));
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                analysisListener.onConstructorFound(constructor);
                methodBuilders.add(new JavaConstructor.Builder().withConstructor(constructor));
            }
            methodBuilders.add(new JavaStaticInitializer.Builder());
            return this;
        }

        public JavaClass build() {
            return new JavaClass(this);
        }
    }

    interface TypeAnalysisListener {
        void onMethodFound(Method method);

        void onConstructorFound(Constructor<?> constructor);

        TypeAnalysisListener NO_OP = new TypeAnalysisListener() {
            @Override
            public void onMethodFound(Method method) {
            }

            @Override
            public void onConstructorFound(Constructor<?> constructor) {
            }
        };
    }
}
