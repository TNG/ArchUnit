package com.tngtech.archunit.core;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import org.assertj.core.util.Files;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static org.assertj.core.util.Files.temporaryFolderPath;
import static org.assertj.core.util.Strings.concat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    private static final Random random = new Random();

    /**
     * NOTE: The resolution of {@link Files#newTemporaryFolder()}, using {@link System#currentTimeMillis()}
     * is not good enough and makes tests flaky.
     */
    public static File newTemporaryFolder() {
        String folderName = "archtmp" + System.nanoTime() + random.nextLong();
        File folder = new File(concat(temporaryFolderPath(), folderName));
        if (folder.exists()) {
            Files.delete(folder);
        }
        checkArgument(folder.mkdirs(), "Folder %s already exists", folder.getAbsolutePath());
        folder.deleteOnExit();
        return folder;
    }

    public static Object invoke(Method method, Object owner, Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(owner, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaMethod javaMethodViaReflection(Class<?> owner, String name, Class<?>... args) {
        return javaMethodViaReflection(javaClassViaReflection(owner), name, args);
    }

    public static JavaMethod javaMethodViaReflection(JavaClass clazz, String name, Class<?>... args) {
        try {
            return javaMethodViaReflection(clazz, clazz.reflect().getDeclaredMethod(name, args));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaMethod javaMethodViaReflection(JavaClass clazz, Method method) {
        return new JavaMethod.Builder()
                .withReturnType(JavaType.From.name(method.getReturnType().getName()))
                .withParameters(typesFrom(method.getParameterTypes()))
                .withName(method.getName())
                .withDescriptor(Type.getMethodDescriptor(method))
                .withAnnotations(javaAnnotationBuildersFrom(method.getAnnotations()))
                .withModifiers(JavaModifier.getModifiersForMethod(method.getModifiers()))
                .build(clazz, simpleImportedClasses());
    }

    static ImportedTestClasses simpleImportedClasses() {
        return new ImportedTestClasses();
    }

    public static JavaClassList javaClassList(Class<?>... types) {
        List<JavaClass> classes = new ArrayList<>();
        for (Class<?> type : types) {
            classes.add(javaClassViaReflection(type));
        }
        return new JavaClassList(classes);
    }

    public static FieldAccessTarget fieldAccessTarget(Class<?> ownerType, String fieldName, Class<?> fieldType) {
        return fieldAccessTarget(ownerType, fieldName, javaClassViaReflection(fieldType));
    }

    public static FieldAccessTarget fieldAccessTarget(Class<?> ownerType, String fieldName, JavaClass fieldType) {
        return new FieldAccessTarget(javaClassViaReflection(ownerType), fieldName, fieldType,
                Suppliers.ofInstance(Optional.<JavaField>absent()));
    }

    private static class ImportedTestClasses implements ImportedClasses.ByTypeName {
        private final Map<String, JavaClass> imported = new HashMap<>();

        void register(JavaClass clazz) {
            imported.put(clazz.getName(), clazz);
        }

        Set<JavaClass> getAll() {
            return ImmutableSet.copyOf(imported.values());
        }

        @Override
        public boolean contain(String typeName) {
            return imported.containsKey(typeName);
        }

        @Override
        public JavaClass get(String typeName) {
            return contain(typeName) ? imported.get(typeName) : importNew(classForName(typeName));
        }

        private JavaClass importNew(Class<?> owner) {
            JavaClass result = javaClassFor(owner);
            imported.put(result.getName(), result);
            return result;
        }
    }

    private static JavaClass javaClassFor(Class<?> owner) {
        return new JavaClass.Builder()
                .withType(JavaType.From.name(owner.getName()))
                .withInterface(owner.isInterface())
                .withModifiers(JavaModifier.getModifiersForClass(owner.getModifiers()))
                .build();
    }

    public static JavaClass javaClassViaReflection(Class<?> owner) {
        return getOnlyElement(javaClassesViaReflection(owner));
    }

    public static JavaField javaFieldViaReflection(Field field, JavaClass owner) {
        return new JavaField.Builder()
                .withName(field.getName())
                .withDescriptor(Type.getDescriptor(field.getType()))
                .withAnnotations(javaAnnotationBuildersFrom(field.getAnnotations()))
                .withModifiers(JavaModifier.getModifiersForField(field.getModifiers()))
                .withType(JavaType.From.name(field.getType().getName()))
                .build(owner, simpleImportedClasses());
    }

    public static JavaField javaFieldViaReflection(Class<?> owner, String name) {
        return javaFieldViaReflection(javaClassViaReflection(owner), name);
    }

    public static JavaField javaFieldViaReflection(JavaClass owner, String name) {
        try {
            Field field = owner.reflect().getDeclaredField(name);
            return javaFieldViaReflection(field, owner);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaClasses javaClassesViaReflection(Class<?>... classes) {
        final ImportedTestClasses importedClasses = simpleImportedClasses();
        final Map<String, JavaClass> result = new HashMap<>();
        for (Class<?> aClass : classes) {
            JavaClass newClass = simulateImport(aClass, importedClasses);
            result.put(newClass.getName(), newClass);
        }

        ImportContext context = simulateContextForCompletion(importedClasses);
        for (JavaClass javaClass : result.values()) {
            javaClass.completeClassHierarchyFrom(context);
        }
        return JavaClasses.of(result, context);
    }

    private static ImportContext simulateContextForCompletion(final ImportedTestClasses importedClasses) {
        ImportContext context = mock(ImportContext.class);
        when(context.createSuperClass(any(JavaClass.class))).thenAnswer(new Answer<Optional<JavaClass>>() {
            @Override
            public Optional<JavaClass> answer(InvocationOnMock invocation) throws Throwable {
                Class<?> clazz = classForName(((JavaClass) invocation.getArguments()[0]).getName());
                return clazz.getSuperclass() != null ?
                        Optional.of(importedClasses.get(clazz.getSuperclass().getName())) :
                        Optional.<JavaClass>absent();
            }
        });
        when(context.createInterfaces(any(JavaClass.class))).thenAnswer(new Answer<Set<JavaClass>>() {
            @Override
            public Set<JavaClass> answer(InvocationOnMock invocation) throws Throwable {
                Class<?> clazz = classForName(((JavaClass) invocation.getArguments()[0]).getName());
                ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
                for (Class<?> iface : clazz.getInterfaces()) {
                    result.add(importedClasses.get(iface.getName()));
                }
                return result.build();
            }
        });
        when(context.getJavaClassWithType(anyString())).thenAnswer(new Answer<JavaClass>() {
            @Override
            public JavaClass answer(InvocationOnMock invocation) throws Throwable {
                String typeName = (String) invocation.getArguments()[0];
                return importedClasses.get(typeName);
            }
        });
        when(context.createEnclosingClass(any(JavaClass.class))).thenAnswer(new Answer<Optional<JavaClass>>() {
            @Override
            public Optional<JavaClass> answer(InvocationOnMock invocation) throws Throwable {
                Class<?> clazz = classForName(((JavaClass) invocation.getArguments()[0]).getName());
                return clazz.getEnclosingClass() != null ?
                        Optional.of(importedClasses.get(clazz.getEnclosingClass().getName())) :
                        Optional.<JavaClass>absent();
            }
        });
        return context;
    }

    private static JavaClass simulateImport(Class<?> owner, ImportedTestClasses importedClasses) {
        JavaClass javaClass = javaClassFor(owner);
        importedClasses.register(javaClass);
        ImportContext context = simulateImportContext(owner, importedClasses);
        javaClass.completeMembers(context);
        return javaClass;
    }

    private static ImportContext simulateImportContext(final Class<?> inputClass, final ImportedClasses.ByTypeName importedClasses) {
        return new ImportContextStub() {
            @Override
            public Set<JavaField> createFields(JavaClass owner) {
                return finish(fieldBuildersFor(inputClass, importedClasses), owner, importedClasses);
            }

            @Override
            public Set<JavaMethod> createMethods(JavaClass owner) {
                return finish(methodBuildersFor(inputClass, importedClasses), owner, importedClasses);
            }

            @Override
            public Set<JavaConstructor> createConstructors(JavaClass owner) {
                return finish(constructorBuildersFor(inputClass, importedClasses), owner, importedClasses);
            }

            @Override
            public Map<String, JavaAnnotation> createAnnotations(JavaClass owner) {
                return annotationsFor(inputClass, importedClasses);
            }
        };
    }

    private static ImmutableMap<String, JavaAnnotation> annotationsFor(Class<?> inputClass, ImportedClasses.ByTypeName importedClasses) {
        return FluentIterable.of(javaAnnotationsFrom(inputClass.getAnnotations(), importedClasses))
                .uniqueIndex(new Function<JavaAnnotation, String>() {
                    @Override
                    public String apply(JavaAnnotation input) {
                        return input.getType().getName();
                    }
                });
    }

    private static Set<BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuildersFor(Class<?> inputClass, ImportedClasses.ByTypeName importedClasses) {
        final Set<BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuilders = new HashSet<>();
        for (Field field : inputClass.getDeclaredFields()) {
            fieldBuilders.add(new JavaField.Builder()
                    .withName(field.getName())
                    .withDescriptor(Type.getDescriptor(field.getType()))
                    .withAnnotations(javaAnnotationBuildersFrom(field.getAnnotations(), importedClasses))
                    .withModifiers(JavaModifier.getModifiersForField(field.getModifiers()))
                    .withType(JavaType.From.name(field.getType().getName())));
        }
        return fieldBuilders;
    }

    private static Set<BuilderWithBuildParameter<JavaClass, JavaMethod>> methodBuildersFor(Class<?> inputClass, ImportedClasses.ByTypeName importedClasses) {
        final Set<BuilderWithBuildParameter<JavaClass, JavaMethod>> methodBuilders = new HashSet<>();
        for (Method method : inputClass.getDeclaredMethods()) {
            methodBuilders.add(new JavaMethod.Builder()
                    .withReturnType(JavaType.From.name(method.getReturnType().getName()))
                    .withParameters(typesFrom(method.getParameterTypes()))
                    .withName(method.getName())
                    .withDescriptor(Type.getMethodDescriptor(method))
                    .withAnnotations(javaAnnotationBuildersFrom(method.getAnnotations(), importedClasses))
                    .withModifiers(JavaModifier.getModifiersForMethod(method.getModifiers())));
        }
        return methodBuilders;
    }

    private static Set<BuilderWithBuildParameter<JavaClass, JavaConstructor>> constructorBuildersFor(Class<?> inputClass, ImportedClasses.ByTypeName importedClasses) {
        final Set<BuilderWithBuildParameter<JavaClass, JavaConstructor>> constructorBuilders = new HashSet<>();
        for (Constructor<?> constructor : inputClass.getDeclaredConstructors()) {
            constructorBuilders.add(new JavaConstructor.Builder()
                    .withReturnType(JavaType.From.name(void.class.getName()))
                    .withParameters(typesFrom(constructor.getParameterTypes()))
                    .withName(CONSTRUCTOR_NAME)
                    .withDescriptor(Type.getConstructorDescriptor(constructor))
                    .withAnnotations(javaAnnotationBuildersFrom(constructor.getAnnotations(), importedClasses))
                    .withModifiers(JavaModifier.getModifiersForMethod(constructor.getModifiers())));
        }
        return constructorBuilders;
    }

    private static <T> Set<T> finish(Set<BuilderWithBuildParameter<JavaClass, T>> builders, JavaClass owner, ImportedClasses.ByTypeName importedClasses) {
        ImmutableSet.Builder<T> result = ImmutableSet.builder();
        for (BuilderWithBuildParameter<JavaClass, T> builder : builders) {
            result.add(builder.build(owner, importedClasses));
        }
        return result.build();
    }

    private static List<JavaType> typesFrom(Class<?>[] classes) {
        List<JavaType> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            result.add(JavaType.From.name(clazz.getName()));
        }
        return result;
    }

    public static AccessesSimulator simulateCall() {
        return new AccessesSimulator();
    }

    public static DescribedPredicate<Object> predicateWithDescription(String description) {
        return DescribedPredicate.alwaysTrue().as(description);
    }

    public static <E extends Enum<?>> JavaEnumConstant enumConstant(E value) {
        return new JavaEnumConstant(simulateImport(value.getDeclaringClass(), simpleImportedClasses()), value.name());
    }

    static FieldAccessTarget targetFrom(JavaField field) {
        return new FieldAccessTarget(
                field.getOwner(),
                field.getName(),
                field.getType(),
                Suppliers.ofInstance(Optional.of(field)));
    }

    static ConstructorCallTarget targetFrom(JavaConstructor target) {
        return new ConstructorCallTarget(
                target.getOwner(),
                target.getParameters(),
                target.getReturnType(), Suppliers.ofInstance(Optional.of(target)));
    }

    static MethodCallTarget resolvedTargetFrom(JavaMethod target) {
        return resolvedTargetFrom(target, Suppliers.ofInstance(Collections.singleton(target)));
    }

    static MethodCallTarget unresolvedTargetFrom(JavaMethod target) {
        return resolvedTargetFrom(target, Suppliers.ofInstance(Collections.<JavaMethod>emptySet()));
    }

    private static MethodCallTarget resolvedTargetFrom(JavaMethod target, Supplier<Set<JavaMethod>> resolveSupplier) {
        return new MethodCallTarget(
                target.getOwner(),
                target.getName(),
                target.getParameters(),
                target.getReturnType(),
                resolveSupplier);
    }

    static Class[] asClasses(List<JavaClass> parameters) {
        List<Class> result = new ArrayList<>();
        for (JavaClass javaClass : parameters) {
            result.add(javaClass.reflect());
        }
        return result.toArray(new Class[result.size()]);
    }

    public static Class<?> classForName(String name) {
        return JavaType.From.name(name).resolveClass();
    }

    private static Set<JavaAnnotation.Builder> javaAnnotationBuildersFrom(Annotation[] reflectionAnnotations) {
        return javaAnnotationBuildersFrom(reflectionAnnotations, simpleImportedClasses());
    }

    private static Set<JavaAnnotation.Builder> javaAnnotationBuildersFrom(Annotation[] reflectionAnnotations, ImportedClasses.ByTypeName importedClasses) {
        ImmutableSet.Builder<JavaAnnotation.Builder> result = ImmutableSet.builder();
        for (Annotation annotation : reflectionAnnotations) {
            result.add(javaAnnotationBuilderFrom(annotation, importedClasses));
        }
        return result.build();
    }

    private static Map<String, Object> mapOf(Annotation annotation, ImportedClasses.ByTypeName importedClasses) {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            result.put(method.getName(), get(annotation, method.getName(), importedClasses));
        }
        return result.build();
    }

    private static Object get(Annotation annotation, String methodName, ImportedClasses.ByTypeName importedClasses) {
        try {
            Object result = annotation.annotationType().getMethod(methodName).invoke(annotation);
            if (result instanceof Class) {
                return importedClasses.get(((Class<?>) result).getName());
            }
            if (result instanceof Class[]) {
                List<JavaClass> classes = javaClassesFrom((Class<?>[]) result, importedClasses);
                return classes.toArray(new JavaClass[classes.size()]);
            }
            if (result instanceof Enum<?>) {
                return enumConstant((Enum) result);
            }
            if (result instanceof Enum[]) {
                return enumConstants((Enum[]) result);
            }
            if (result instanceof Annotation) {
                return javaAnnotationFrom((Annotation) result);
            }
            if (result instanceof Annotation[]) {
                return javaAnnotationsFrom((Annotation[]) result);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<JavaClass> javaClassesFrom(Class<?>[] classes, ImportedClasses.ByTypeName importedClasses) {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        for (Class<?> c : classes) {
            result.add(importedClasses.get(c.getName()));
        }
        return result.build();
    }

    private static JavaEnumConstant[] enumConstants(Enum[] enums) {
        List<JavaEnumConstant> result = new ArrayList<>();
        for (Enum e : enums) {
            result.add(enumConstant(e));
        }
        return result.toArray(new JavaEnumConstant[result.size()]);
    }

    static JavaAnnotation javaAnnotationFrom(Annotation annotation) {
        return javaAnnotationFrom(annotation, simpleImportedClasses());
    }

    private static JavaAnnotation javaAnnotationFrom(Annotation annotation, ImportedClasses.ByTypeName importedClasses) {
        return javaAnnotationBuilderFrom(annotation, importedClasses).build(importedClasses);
    }

    private static JavaAnnotation.Builder javaAnnotationBuilderFrom(Annotation annotation, ImportedClasses.ByTypeName importedClasses) {
        JavaAnnotation.Builder builder = new JavaAnnotation.Builder().withType(JavaType.From.name(annotation.annotationType().getName()));
        for (Map.Entry<String, Object> entry : mapOf(annotation, importedClasses).entrySet()) {
            builder.addProperty(entry.getKey(), JavaAnnotation.ValueBuilder.ofFinished(entry.getValue()));
        }
        return builder;
    }

    private static JavaAnnotation[] javaAnnotationsFrom(Annotation[] annotations) {
        return javaAnnotationsFrom(annotations, simpleImportedClasses());
    }

    private static JavaAnnotation[] javaAnnotationsFrom(Annotation[] annotations, ImportedClasses.ByTypeName importedClasses) {
        List<JavaAnnotation> result = new ArrayList<>();
        for (Annotation a : annotations) {
            result.add(javaAnnotationFrom(a, importedClasses));
        }
        return result.toArray(new JavaAnnotation[result.size()]);
    }

    public static class AccessesSimulator {
        private final Set<AccessRecord<MethodCallTarget>> targets = new HashSet<>();

        public AccessSimulator from(JavaMethod method, int lineNumber) {
            return new AccessSimulator(targets, method, lineNumber);
        }

        public AccessSimulator from(Class<?> clazz, String methodName, Class<?>... params) {
            return new AccessSimulator(targets, javaMethodViaReflection(clazz, methodName, params), 0);
        }

        public AccessSimulator from(JavaClass clazz, String methodName, Class<?>... params) {
            return from(clazz.getMethod(methodName, params), 0);
        }
    }

    public static class AccessSimulator {
        private final Set<AccessRecord<MethodCallTarget>> targets;
        private final JavaMethod method;
        private final int lineNumber;

        private AccessSimulator(Set<AccessRecord<MethodCallTarget>> targets, JavaMethod method, int lineNumber) {
            this.targets = targets;
            this.method = method;
            this.lineNumber = lineNumber;
        }

        public JavaMethodCall to(JavaMethod target) {
            return to(resolvedTargetFrom(target));
        }

        private JavaMethodCall to(MethodCallTarget methodCallTarget) {
            targets.add(new TestAccessRecord<>(methodCallTarget));
            ClassGraphCreator context = mock(ClassGraphCreator.class);
            when(context.getMethodCallRecordsFor(method)).thenReturn(ImmutableSet.copyOf(targets));
            method.completeFrom(context);
            return getCallToTarget(methodCallTarget);
        }

        public JavaCall<?> to(Class<?> clazz, String methodName, Class<?>... params) {
            return to(resolvedTargetFrom(javaMethodViaReflection(clazz, methodName, params)));
        }

        public JavaCall<?> toUnresolved(Class<?> clazz, String methodName, Class<?>... params) {
            return to(unresolvedTargetFrom(javaMethodViaReflection(clazz, methodName, params)));
        }

        private JavaMethodCall getCallToTarget(MethodCallTarget callTarget) {
            Set<JavaMethodCall> matchingCalls = new HashSet<>();
            for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                if (call.getTarget().equals(callTarget)) {
                    matchingCalls.add(call);
                }
            }
            return getOnlyElement(matchingCalls);
        }

        public void to(JavaField target, AccessType accessType) {
            ClassGraphCreator context = mock(ClassGraphCreator.class);
            when(context.getFieldAccessRecordsFor(method))
                    .thenReturn(ImmutableSet.<FieldAccessRecord>of(new TestFieldAccessRecord(target, accessType)));
            method.completeFrom(context);
        }

        private class TestFieldAccessRecord extends TestAccessRecord<FieldAccessTarget> implements FieldAccessRecord {
            private final AccessType accessType;

            private TestFieldAccessRecord(JavaField target, AccessType accessType) {
                super(targetFrom(target));
                this.accessType = accessType;
            }

            @Override
            public AccessType getAccessType() {
                return accessType;
            }
        }

        private class TestAccessRecord<T extends AccessTarget> implements AccessRecord<T> {
            private final T target;

            TestAccessRecord(T target) {
                this.target = target;
            }

            @Override
            public JavaCodeUnit getCaller() {
                return method;
            }

            @Override
            public T getTarget() {
                return target;
            }

            @Override
            public int getLineNumber() {
                return lineNumber;
            }
        }
    }

    private static class ImportContextStub implements ImportContext {
        @Override
        public JavaClass getJavaClassWithType(String name) {
            throw new UnsupportedOperationException("Stub can't resolve type name " + name);
        }

        @Override
        public Optional<JavaClass> createSuperClass(JavaClass owner) {
            return Optional.absent();
        }

        @Override
        public Set<JavaClass> createInterfaces(JavaClass owner) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaField> createFields(JavaClass owner) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaMethod> createMethods(JavaClass owner) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaConstructor> createConstructors(JavaClass owner) {
            return Collections.emptySet();
        }

        @Override
        public Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner) {
            return Optional.absent();
        }

        @Override
        public Map<String, JavaAnnotation> createAnnotations(JavaClass owner) {
            return Collections.emptyMap();
        }

        @Override
        public Optional<JavaClass> createEnclosingClass(JavaClass owner) {
            return Optional.absent();
        }

        @Override
        public Set<FieldAccessRecord> getFieldAccessRecordsFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public Set<AccessRecord<MethodCallTarget>> getMethodCallRecordsFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public Set<AccessRecord<ConstructorCallTarget>> getConstructorCallRecordsFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }
    }
}