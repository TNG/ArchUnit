package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static com.tngtech.archunit.core.ReflectionUtils.namesOf;

public class JavaClass implements HasName, HasAnnotations {
    private final JavaType javaType;
    private Set<JavaField> fields = new HashSet<>();
    private Set<JavaCodeUnit> codeUnits = new HashSet<>();
    private Set<JavaMethod> methods = new HashSet<>();
    private Set<JavaConstructor> constructors = new HashSet<>();
    private Optional<JavaStaticInitializer> staticInitializer = Optional.absent();
    private Optional<JavaClass> superClass = Optional.absent();
    private final Set<JavaClass> interfaces = new HashSet<>();
    private final Set<JavaClass> subClasses = new HashSet<>();
    private Optional<JavaClass> enclosingClass = Optional.absent();
    private Map<String, JavaAnnotation> annotations = new HashMap<>();
    private boolean isInterface;
    // FIXME: JavaClass should have modifiers

    private JavaClass(Builder builder) {
        javaType = checkNotNull(builder.javaType);
        isInterface = builder.isInterface;
    }

    @Override
    public String getName() {
        return javaType.getName();
    }

    public String getSimpleName() {
        return javaType.getSimpleName();
    }

    public String getPackage() {
        return javaType.getPackage();
    }

    public boolean isInterface() {
        return isInterface;
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
        return annotations.containsKey(annotation.getName());
    }

    /**
     * @param type The type of the {@link Annotation} to retrieve
     * @return The {@link Annotation} representing the given annotation type
     * @throws IllegalArgumentException if the class is note annotated with the given type
     * @see #isAnnotatedWith(Class)
     * @see #getAnnotationOfType(Class)
     */
    public <A extends Annotation> A getReflectionAnnotation(Class<A> type) {
        return getAnnotationOfType(type).as(type);
    }

    /**
     * @param type A given annotation type to match {@link JavaAnnotation JavaAnnotations} against
     * @return The {@link JavaAnnotation} representing the given annotation type
     * @throws IllegalArgumentException if the class is note annotated with the given type
     * @see #isAnnotatedWith(Class)
     * @see #tryGetAnnotationOfType(Class)
     */
    @Override
    public JavaAnnotation getAnnotationOfType(Class<? extends Annotation> type) {
        return tryGetAnnotationOfType(type).getOrThrow(new IllegalArgumentException(
                String.format("Type %s is not annotated with @%s", getSimpleName(), type.getSimpleName())));
    }

    @Override
    public Set<JavaAnnotation> getAnnotations() {
        return ImmutableSet.copyOf(annotations.values());
    }

    /**
     * @param type A given annotation type to match {@link JavaAnnotation JavaAnnotations} against
     * @return An {@link Optional} containing a {@link JavaAnnotation} representing the given annotation type,
     * if this class is annotated with the given type, otherwise Optional.absent()
     * @see #isAnnotatedWith(Class)
     * @see #getAnnotationOfType(Class)
     */
    @Override
    public Optional<JavaAnnotation> tryGetAnnotationOfType(Class<? extends Annotation> type) {
        return Optional.fromNullable(annotations.get(type.getName()));
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

    public Set<JavaField> getAllFields() {
        ImmutableSet.Builder<JavaField> result = ImmutableSet.builder();
        for (JavaClass javaClass : getClassHierarchy()) {
            result.addAll(javaClass.getFields());
        }
        return result.build();
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

    public Set<JavaCodeUnit> getCodeUnits() {
        return codeUnits;
    }

    /**
     * @param name       The name of the code unit, can be a method name, but also
     *                   {@link JavaConstructor#CONSTRUCTOR_NAME CONSTRUCTOR_NAME}
     *                   or {@link JavaStaticInitializer#STATIC_INITIALIZER_NAME STATIC_INITIALIZER_NAME}
     * @param parameters The parameter signature of the method specified as {@link Class Class} Objects
     * @return A code unit (method, constructor or static initializer) with the given signature
     */
    public JavaCodeUnit getCodeUnitWithParameterTypes(String name, Class<?>... parameters) {
        return getCodeUnitWithParameterTypes(name, ImmutableList.copyOf(parameters));
    }

    /**
     * Same as {@link #getCodeUnitWithParameterTypes(String, Class[])}, but with parameter signature specified as full class names
     */
    public JavaCodeUnit getCodeUnitWithParameterTypeNames(String name, String... parameters) {
        return getCodeUnitWithParameterTypeNames(name, ImmutableList.copyOf(parameters));
    }

    public JavaCodeUnit getCodeUnitWithParameterTypes(String name, List<Class<?>> parameters) {
        return getCodeUnitWithParameterTypeNames(name, namesOf(parameters));
    }

    public JavaCodeUnit getCodeUnitWithParameterTypeNames(String name, List<String> parameters) {
        return findMatchingCodeUnit(codeUnits, name, parameters);
    }

    private <T extends JavaCodeUnit> T findMatchingCodeUnit(Set<T> codeUnits, String name, List<String> parameters) {
        return tryFindMatchingCodeUnit(codeUnits, name, parameters).getOrThrow(new IllegalArgumentException("No code unit with name '" + name + "' and parameters " + parameters +
                " in codeUnits " + codeUnits + " of class " + getName()));
    }

    private <T extends JavaCodeUnit> Optional<T> tryFindMatchingCodeUnit(Set<T> codeUnits, String name, List<String> parameters) {
        for (T codeUnit : codeUnits) {
            if (name.equals(codeUnit.getName()) && parameters.equals(codeUnit.getParameters().getNames())) {
                return Optional.of(codeUnit);
            }
        }
        return Optional.absent();
    }

    public JavaMethod getMethod(String name, Class<?>... parameters) {
        return findMatchingCodeUnit(methods, name, namesOf(parameters));
    }

    public Set<JavaMethod> getMethods() {
        return methods;
    }

    public Set<JavaMethod> getAllMethods() {
        ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
        for (JavaClass javaClass : concat(getClassHierarchy(), getAllInterfaces())) {
            result.addAll(javaClass.getMethods());
        }
        return result.build();
    }

    public JavaConstructor getConstructor(Class<?>... parameters) {
        return findMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, namesOf(parameters));
    }

    public Set<JavaConstructor> getConstructors() {
        return constructors;
    }

    public Set<JavaConstructor> getAllConstructors() {
        ImmutableSet.Builder<JavaConstructor> result = ImmutableSet.builder();
        for (JavaClass javaClass : getClassHierarchy()) {
            result.addAll(javaClass.getConstructors());
        }
        return result.build();
    }

    public Optional<JavaStaticInitializer> getStaticInitializer() {
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
        for (JavaCodeUnit codeUnit : codeUnits) {
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
        return Sets.union(getMethodCallsFromSelf(), getConstructorCallsFromSelf());
    }

    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getMethodCallsFromSelf());
        }
        return result.build();
    }

    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
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

    /**
     * Resolves the respective {@link Class} from the classpath.<br/>
     * NOTE: This method will throw an exception, if the respective {@link Class} or any of its dependencies
     * can not be found on the classpath.
     *
     * @return The {@link Class} equivalent to this {@link JavaClass}
     */
    @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
    public Class<?> reflect() {
        return classForName(getName());
    }

    void completeClassHierarchyFrom(ImportContext context) {
        completeSuperClassFrom(context);
        completeInterfacesFrom(context);
    }

    private void completeSuperClassFrom(ImportContext context) {
        superClass = context.createSuperClass(this);
        if (superClass.isPresent()) {
            superClass.get().subClasses.add(this);
        }
    }

    private void completeInterfacesFrom(ImportContext context) {
        interfaces.addAll(context.createInterfaces(this));
        for (JavaClass i : interfaces) {
            i.subClasses.add(this);
        }
    }

    void completeMembers(ImportContext context) {
        fields = context.createFields(this);
        methods = context.createMethods(this);
        constructors = context.createConstructors(this);
        staticInitializer = context.createStaticInitializer(this);
        codeUnits = ImmutableSet.<JavaCodeUnit>builder()
                .addAll(methods).addAll(constructors).addAll(staticInitializer.asSet())
                .build();
        this.annotations = context.createAnnotations(this);
    }

    CompletionProcess completeFrom(ImportContext context) {
        enclosingClass = context.createEnclosingClass(this);
        return new CompletionProcess();
    }

    @Override
    public String toString() {
        return "JavaClass{name='" + javaType.getName() + "\'}";
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
        @MayResolveTypesViaReflection(reason = "This is not part of the import and a specific decision to rely on the classpath")
        public Class<?> apply(JavaClass input) {
            return input.reflect();
        }
    };

    class CompletionProcess {
        AccessContext.Part completeCodeUnitsFrom(ImportContext context) {
            AccessContext.Part part = new AccessContext.Part();
            for (JavaCodeUnit codeUnit : codeUnits) {
                part.mergeWith(codeUnit.completeFrom(context));
            }
            return part;
        }
    }

    static final class Builder {
        private JavaType javaType;
        private boolean isInterface;

        @SuppressWarnings("unchecked")
        Builder withType(JavaType javaType) {
            this.javaType = javaType;
            return this;
        }

        Builder withInterface(boolean isInterface) {
            this.isInterface = isInterface;
            return this;
        }

        JavaClass build() {
            return new JavaClass(this);
        }
    }
}
