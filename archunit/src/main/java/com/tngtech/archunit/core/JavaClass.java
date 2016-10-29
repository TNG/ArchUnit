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
import static com.tngtech.archunit.core.ReflectionUtils.classForName;

public class JavaClass implements HasName {
    private final TypeDetails typeDetails;
    private final Set<JavaField> fields;
    private final Set<JavaCodeUnit<?, ?>> codeUnits;
    private final Set<JavaMethod> methods;
    private final Set<JavaConstructor> constructors;
    private final JavaStaticInitializer staticInitializer;
    private Optional<JavaClass> superClass = Optional.absent();
    private final Set<JavaClass> interfaces = new HashSet<>();
    private final Set<JavaClass> subClasses = new HashSet<>();
    private Optional<JavaClass> enclosingClass = Optional.absent();

    private JavaClass(Builder builder) {
        typeDetails = checkNotNull(builder.typeDetails);
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
        return typeDetails.getName();
    }

    public String getSimpleName() {
        return typeDetails.getSimpleName();
    }

    public String getPackage() {
        return typeDetails.getPackage();
    }

    public boolean isInterface() {
        return typeDetails.isInterface();
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

    public Set<JavaClass> getInterfaces() {
        return interfaces;
    }

    public Set<JavaClass> getAllInterfaces() {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (JavaClass i : interfaces) {
            result.add(i);
            result.addAll(i.getAllInterfaces());
        }
        if (superClass.isPresent()) {
            result.addAll(superClass.get().getAllInterfaces());
        }
        return result.build();
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

    public Set<JavaMethod> getAllMethods() {
        ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
        for (JavaClass javaClass : getClassHierarchy()) {
            result.addAll(javaClass.getMethods());
        }
        return result.build();
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

    public Class<?> reflect() {
        return classForName(getName());
    }

    void completeClassHierarchyFrom(ClassFileImportContext context) {
        completeSuperClassFrom(context);
        completeInterfacesFrom(context);
    }

    private void completeSuperClassFrom(ClassFileImportContext context) {
        superClass = findClass(typeDetails.getSuperclass(), context);
        if (superClass.isPresent()) {
            superClass.get().subClasses.add(this);
        }
    }

    private void completeInterfacesFrom(ClassFileImportContext context) {
        for (Class<?> i : typeDetails.getInterfaces()) {
            interfaces.addAll(findClass(i, context).asSet());
        }
        for (JavaClass i : interfaces) {
            i.subClasses.add(this);
        }
    }

    private static Optional<JavaClass> findClass(Class<?> clazz, ClassFileImportContext context) {
        return clazz != null ? context.tryGetJavaClassWithType(clazz.getName()) : Optional.<JavaClass>absent();
    }

    CompletionProcess completeFrom(ClassFileImportContext context) {
        enclosingClass = findClass(typeDetails.getEnclosingClass(), context);
        return new CompletionProcess();
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeDetails);
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
        return Objects.equals(this.typeDetails, other.typeDetails);
    }

    @Override
    public String toString() {
        return "JavaClass{name='" + typeDetails.getName() + "\'}";
    }

    public Set<JavaFieldAccess> getFieldAccessesToSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaField field : fields) {
            result.addAll(field.getAccessesToSelf());
        }
        return result.build();
    }

    public static DescribedPredicate<JavaClass> withType(final Class<?> type) {
        return DescribedPredicate.<Class<?>>equalTo(type).onResultOf(REFLECT).as("with type " + type.getName());
    }

    public static DescribedPredicate<Class<?>> reflectionAssignableTo(final Class<?> type) {
        checkNotNull(type);
        return new DescribedPredicate<Class<?>>("assignable to " + type.getName()) {
            @Override
            public boolean apply(Class<?> input) {
                return type.isAssignableFrom(input);
            }
        };
    }

    public static DescribedPredicate<Class<?>> reflectionAssignableFrom(final Class<?> type) {
        checkNotNull(type);
        return new DescribedPredicate<Class<?>>("assignable from " + type.getName()) {
            @Override
            public boolean apply(Class<?> input) {
                return input.isAssignableFrom(type);
            }
        };
    }

    public static DescribedPredicate<JavaClass> assignableTo(final Class<?> type) {
        return reflectionAssignableTo(type).onResultOf(REFLECT);
    }

    public static DescribedPredicate<JavaClass> assignableFrom(final Class<?> type) {
        return reflectionAssignableFrom(type).onResultOf(REFLECT);
    }

    public static final DescribedPredicate<JavaClass> INTERFACES = new DescribedPredicate<JavaClass>("interfaces") {
        @Override
        public boolean apply(JavaClass input) {
            return input.isInterface();
        }
    };

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
        private TypeDetails typeDetails;
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
        Builder withType(TypeDetails typeDetails) {
            this.typeDetails = typeDetails;
            for (Field field : typeDetails.getDeclaredFields()) {
                fieldBuilders.add(new JavaField.Builder().withField(field));
            }
            for (Method method : typeDetails.getDeclaredMethods()) {
                analysisListener.onMethodFound(method);
                methodBuilders.add(new JavaMethod.Builder().withMethod(method));
            }
            for (Constructor<?> constructor : typeDetails.getDeclaredConstructors()) {
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
