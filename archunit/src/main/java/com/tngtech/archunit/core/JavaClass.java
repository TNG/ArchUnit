package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.JavaClass.TypeAnalysisListener.NO_OP;

public class JavaClass implements HasName {
    private final Class<?> type;
    private final Set<JavaField> fields;
    private final Set<JavaCodeUnit<?, ?>> codeUnits;
    private final Set<JavaMethod> methods;
    private final Set<JavaConstructor> constructors;
    private final JavaStaticInitializer staticInitializer;
    private Optional<JavaClass> superClass = Optional.absent();
    private final Set<JavaClass> subClasses = new HashSet<>();
    private Optional<JavaClass> enclosingClass = Optional.absent();

    private JavaClass(Builder builder) {
        type = checkNotNull(builder.type);
        fields = build(builder.fieldBuilders, this);
        methods = build(builder.methodBuilders, this);
        constructors = build(builder.constructorBuilders, this);
        staticInitializer = new JavaStaticInitializer.Builder().build(this);
        codeUnits = ImmutableSet.<JavaCodeUnit<?, ?>>builder()
                .addAll(methods).addAll(constructors).add(staticInitializer)
                .build();
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

    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return reflect().isAnnotationPresent(annotation);
    }

    public Optional<JavaClass> getSuperClass() {
        return superClass;
    }

    /**
     * @return The complete class hierarchy, i.e. the class itself and the result of {@link #getAllSuperClasses()}
     */
    public List<JavaClass> getClassHierarchy() {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        result.add(this);
        result.addAll(getAllSuperClasses());
        return result.build();
    }

    /**
     * @return All super classes sorted ascending by distance in the class hierarchy, i.e. first the direct super class,
     * then the super class of the super class and so on. Includes Object.class in the result.
     */
    public List<JavaClass> getAllSuperClasses() {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        JavaClass current = this;
        while (current.getSuperClass().isPresent()) {
            current = current.getSuperClass().get();
            result.add(current);
        }
        return result.build();
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
        return tryGetField(name).getOrThrow(new IllegalArgumentException("No field with name '" + name + " in class " + getName()));
    }

    public Optional<JavaField> tryGetField(String name) {
        for (JavaField field : fields) {
            if (name.equals(field.getName())) {
                return Optional.of(field);
            }
        }
        return Optional.absent();
    }

    public Set<JavaCodeUnit<?, ?>> getCodeUnits() {
        return codeUnits;
    }

    /**
     * @param name       The name of the code unit, can be a method name, but also
     *                   {@link JavaConstructor#CONSTRUCTOR_NAME CONSTRUCTOR_NAME}
     *                   or {@link JavaStaticInitializer#STATIC_INITIALIZER_NAME STATIC_INITIALIZER_NAME}
     * @param parameters The parameter signature of the method
     * @return A code unit (method, constructor or static initializer) with the given signature
     */
    public JavaCodeUnit<?, ?> getCodeUnit(String name, Class<?>... parameters) {
        return findMatchingCodeUnit(codeUnits, name, parameters);
    }

    private <T extends JavaCodeUnit<?, ?>> T findMatchingCodeUnit(Set<T> methods, String name, Class<?>[] parameters) {
        return findMatchingCodeUnit(methods, name, newArrayList(parameters));
    }

    private <T extends JavaCodeUnit<?, ?>> T findMatchingCodeUnit(Set<T> codeUnits, String name, List<Class<?>> parameters) {
        return tryFindMatchingCodeUnit(codeUnits, name, parameters).getOrThrow(new IllegalArgumentException("No code unit with name '" + name + "' and parameters " + parameters +
                " in codeUnits " + codeUnits + " of class " + getName()));
    }

    private <T extends JavaCodeUnit<?, ?>> Optional<T> tryFindMatchingCodeUnit(Set<T> codeUnits, String name, List<Class<?>> parameters) {
        for (T codeUnit : codeUnits) {
            if (name.equals(codeUnit.getName()) && parameters.equals(codeUnit.getParameters())) {
                return Optional.of(codeUnit);
            }
        }
        return Optional.absent();
    }

    public JavaMethod getMethod(String name, Class<?>... parameters) {
        return findMatchingCodeUnit(methods, name, parameters);
    }

    public Set<JavaMethod> getMethods() {
        return methods;
    }

    public JavaConstructor getConstructor(Class<?>... parameters) {
        return findMatchingCodeUnit(constructors, JavaConstructor.CONSTRUCTOR_NAME, parameters);
    }

    public Set<JavaConstructor> getConstructors() {
        return constructors;
    }

    public JavaStaticInitializer getStaticInitializer() {
        return staticInitializer;
    }

    public Set<JavaAccess<?>> getAccessesFromSelf() {
        return Sets.union(getFieldAccessesFromSelf(), getCallsFromSelf());
    }

    /**
     * @return Set of all {@link JavaAccess} in the class hierarchy, as opposed to the accesses this class directly performs.
     */
    public Set<JavaAccess<?>> getAllAccessesFromSelf() {
        ImmutableSet.Builder<JavaAccess<?>> result = ImmutableSet.builder();
        for (JavaClass clazz : getClassHierarchy()) {
            result.addAll(clazz.getAccessesFromSelf());
        }
        return result.build();
    }

    public Set<JavaFieldAccess> getFieldAccessesFromSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaCodeUnit<?, ?> codeUnit : codeUnits) {
            result.addAll(codeUnit.getFieldAccesses());
        }
        return result.build();
    }

    /**
     * Returns all calls of this class to methods or constructors.
     *
     * @see #getMethodCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     */
    public Set<JavaCall<?>> getCallsFromSelf() {
        return Sets.<JavaCall<?>>union(getMethodCallsFromSelf(), getConstructorCallsFromSelf());
    }

    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaCodeUnit<?, ?> codeUnit : codeUnits) {
            result.addAll(codeUnit.getMethodCallsFromSelf());
        }
        return result.build();
    }

    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaCodeUnit<?, ?> codeUnit : codeUnits) {
            result.addAll(codeUnit.getConstructorCallsFromSelf());
        }
        return result.build();
    }

    public Set<Dependency> getDirectDependencies() {
        Set<Dependency> result = new HashSet<>();
        for (JavaAccess<?> access : filterTargetNotSelf(getFieldAccessesFromSelf())) {
            result.add(Dependency.from(access));
        }
        for (JavaAccess<?> call : filterTargetNotSelf(getCallsFromSelf())) {
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

    public Set<JavaFieldAccess> getFieldAccessesToSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaField field : fields) {
            result.addAll(field.getAccessesToSelf());
        }
        return result.build();
    }

    public Set<JavaMethodCall> getMethodCallsToSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaMethod method : methods) {
            result.addAll(method.getCallsOfSelf());
        }
        return result.build();
    }

    public Set<JavaConstructorCall> getConstructorCallsToSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaConstructor constructor : constructors) {
            result.addAll(constructor.getCallsOfSelf());
        }
        return result.build();
    }

    public Set<JavaAccess<?>> getAccessesToSelf() {
        return ImmutableSet.<JavaAccess<?>>builder()
                .addAll(getFieldAccessesToSelf())
                .addAll(getMethodCallsToSelf())
                .addAll(getConstructorCallsToSelf())
                .build();
    }

    public static FluentPredicate<JavaClass> withType(final Class<?> type) {
        return FluentPredicate.<Class<?>>equalTo(type).onResultOf(REFLECT);
    }

    public static FluentPredicate<Class<?>> assignableTo(final Class<?> type) {
        checkNotNull(type);
        return new FluentPredicate<Class<?>>() {
            @Override
            public boolean apply(Class<?> input) {
                return type.isAssignableFrom(input);
            }
        };
    }

    public static FluentPredicate<Class<?>> assignableFrom(final Class<?> type) {
        checkNotNull(type);
        return new FluentPredicate<Class<?>>() {
            @Override
            public boolean apply(Class<?> input) {
                return input.isAssignableFrom(type);
            }
        };
    }

    public static final Function<JavaClass, Class<?>> REFLECT = new Function<JavaClass, Class<?>>() {
        @Override
        public Class<?> apply(JavaClass input) {
            return input.reflect();
        }
    };

    class CompletionProcess {
        AccessCompletion.SubProcess completeCodeUnitsFrom(ClassFileImportContext context) {
            AccessCompletion.SubProcess accessCompletionProcess = new AccessCompletion.SubProcess();
            for (JavaCodeUnit<?, ?> codeUnit : codeUnits) {
                accessCompletionProcess.mergeWith(codeUnit.completeFrom(context));
            }
            return accessCompletionProcess;
        }
    }

    static final class Builder {
        private Class<?> type;
        private final Set<BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuilders = new HashSet<>();
        private final Set<BuilderWithBuildParameter<JavaClass, JavaMethod>> methodBuilders = new HashSet<>();
        private final Set<BuilderWithBuildParameter<JavaClass, JavaConstructor>> constructorBuilders = new HashSet<>();
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
                constructorBuilders.add(new JavaConstructor.Builder().withConstructor(constructor));
            }
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
