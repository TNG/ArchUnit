package com.tngtech.archunit.core;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.ClassFileProcessor.ClassResolverFromClassPath;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import org.assertj.core.util.Files;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.JavaAnnotation.buildAnnotations;
import static org.assertj.core.util.Files.temporaryFolderPath;
import static org.assertj.core.util.Strings.concat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    /**
     * NOTE: The resolution of {@link Files#newTemporaryFolder()}, using {@link System#currentTimeMillis()}
     * is not good enough and makes tests flaky.
     */
    public static File newTemporaryFolder() {
        String folderName = "archtmp" + System.currentTimeMillis();
        File folder = new File(concat(temporaryFolderPath(), folderName));
        if (folder.exists()) {
            Files.delete(folder);
        }
        checkArgument(folder.mkdirs(), "Folder %s already exists", folder.getAbsolutePath());
        folder.deleteOnExit();
        return folder;
    }

    public static Object invoke(Method method, Object owner) {
        try {
            return method.invoke(owner);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaMethod javaMethod(Class<?> owner, String name, Class<?>... args) {
        return javaMethod(javaClass(owner), name, args);
    }

    public static JavaMethod javaMethod(JavaClass clazz, String name, Class<?>... args) {
        try {
            return javaMethod(clazz, clazz.reflect().getDeclaredMethod(name, args));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaMethod javaMethod(JavaClass clazz, Method method) {
        return new JavaMethod.Builder()
                .withReturnType(Type.getType(method.getReturnType()))
                .withParameters(allTypesIn(method.getParameterTypes()))
                .withName(method.getName())
                .withDescriptor(Type.getMethodDescriptor(method))
                .withAnnotations(javaAnnotationBuildersFrom(method.getAnnotations()))
                .withModifiers(JavaModifier.getModifiersFor(method.getModifiers()))
                .build(clazz);
    }

    private static Type[] allTypesIn(Class<?>[] types) {
        Type[] result = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = Type.getType(types[i]);
        }
        return result;
    }

    public static JavaClass javaClass(Class<?> owner) {
        JavaClass javaClass = importSingle(owner);
        ClassGraphCreator context = mock(ClassGraphCreator.class);
        when(context.getJavaClassWithType(anyString())).thenAnswer(new Answer<JavaClass>() {
            @Override
            public JavaClass answer(InvocationOnMock invocation) throws Throwable {
                return javaClass(classForName((String) invocation.getArguments()[0]));
            }
        });
        javaClass.completeClassHierarchyFrom(context);
        javaClass.completeFrom(context);
        for (JavaCodeUnit unit : javaClass.getCodeUnits()) {
            unit.completeFrom(context);
        }
        return javaClass;
    }

    public static JavaField javaField(Field field, JavaClass owner) {
        return new JavaField.Builder()
                .withName(field.getName())
                .withDescriptor(Type.getDescriptor(field.getType()))
                .withAnnotations(javaAnnotationBuildersFrom(field.getAnnotations()))
                .withModifiers(JavaModifier.getModifiersFor(field.getModifiers()))
                .withType(Type.getType(field.getType()))
                .build(owner);
    }

    public static JavaField javaField(Class<?> ownerClass, String name) {
        return javaField(javaClass(ownerClass), name);
    }

    public static JavaField javaField(JavaClass owner, String name) {
        try {
            Field field = owner.reflect().getDeclaredField(name);
            return javaField(field, owner);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaClasses javaClasses(Class<?>... classes) {
        Map<String, JavaClass> result = new HashMap<>();
        for (Class<?> c : classes) {
            result.put(c.getName(), javaClass(c));
        }
        return new JavaClasses(result);
    }

    public static AccessesSimulator simulateCall() {
        return new AccessesSimulator();
    }

    public static DescribedPredicate<Object> predicateWithDescription(String description) {
        return DescribedPredicate.alwaysTrue().as(description);
    }

    public static <E extends Enum<?>> JavaEnumConstant enumConstant(E value) {
        return new JavaEnumConstant(TypeDetails.of(value.getDeclaringClass()), value.name());
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
                Suppliers.ofInstance(Optional.of(target)));
    }

    static MethodCallTarget targetFrom(JavaMethod target) {
        return new MethodCallTarget(
                target.getOwner(),
                target.getName(),
                target.getParameters(),
                target.getReturnType(),
                Suppliers.ofInstance(Collections.singleton(target)));
    }

    static Class[] asClasses(List<TypeDetails> parameters) {
        List<Class> result = new ArrayList<>();
        for (TypeDetails type : parameters) {
            result.add(classForName(type.getName()));
        }
        return result.toArray(new Class[result.size()]);
    }

    public static Class<?> classForName(String name) {
        return ReflectionUtils.classForName(name);
    }

    public static JavaClass importSingle(Class<?> clazz) {
        TestDeclarationHandler declarationHandler = new TestDeclarationHandler();
        JavaClass javaClass = new ClassResolverFromClassPath(declarationHandler).resolve(clazz.getName());
        javaClass.completeClassHierarchyFrom(declarationHandler);
        javaClass.completeMembers(declarationHandler);
        javaClass.completeFrom(declarationHandler);
        return javaClass;
    }

    public static JavaAnnotation javaAnnotationOf(Annotation reflectionAnnotation) {
        return javaAnnotationBuilderFrom(reflectionAnnotation).build();
    }

    public static Set<JavaAnnotation.Builder> javaAnnotationBuildersFrom(Annotation[] reflectionAnnotations) {
        ImmutableSet.Builder<JavaAnnotation.Builder> result = ImmutableSet.builder();
        for (Annotation annotation : reflectionAnnotations) {
            result.add(javaAnnotationBuilderFrom(annotation));
        }
        return result.build();
    }

    private static Map<String, Object> mapOf(Annotation annotation) {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            result.put(method.getName(), get(annotation, method.getName()));
        }
        return result.build();
    }

    private static Object get(Annotation annotation, String methodName) {
        try {
            Object result = annotation.annotationType().getMethod(methodName).invoke(annotation);
            if (result instanceof Class) {
                return TypeDetails.of((Class<?>) result);
            }
            if (result instanceof Class[]) {
                List<TypeDetails> typeDetails = TypeDetails.allOf((Class<?>[]) result);
                return typeDetails.toArray(new TypeDetails[typeDetails.size()]);
            }
            if (result instanceof Enum<?>) {
                return enumConstant((Enum) result);
            }
            if (result instanceof Enum[]) {
                return enumConstants((Enum[]) result);
            }
            if (result instanceof Annotation) {
                return annotation((Annotation) result);
            }
            if (result instanceof Annotation[]) {
                return annotations((Annotation[]) result);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JavaEnumConstant[] enumConstants(Enum[] enums) {
        List<JavaEnumConstant> result = new ArrayList<>();
        for (Enum e : enums) {
            result.add(enumConstant(e));
        }
        return result.toArray(new JavaEnumConstant[result.size()]);
    }

    private static JavaAnnotation annotation(Annotation annotation) {
        return javaAnnotationBuilderFrom(annotation).build();
    }

    private static JavaAnnotation.Builder javaAnnotationBuilderFrom(Annotation annotation) {
        JavaAnnotation.Builder builder = new JavaAnnotation.Builder().withType(Type.getType(annotation.annotationType()));
        for (Map.Entry<String, Object> entry : mapOf(annotation).entrySet()) {
            builder.addProperty(entry.getKey(), JavaAnnotation.ValueBuilder.ofFinished(entry.getValue()));
        }
        return builder;
    }

    private static JavaAnnotation[] annotations(Annotation[] annotations) {
        List<JavaAnnotation> result = new ArrayList<>();
        for (Annotation a : annotations) {
            result.add(annotation(a));
        }
        return result.toArray(new JavaAnnotation[result.size()]);
    }

    public static class AccessesSimulator {
        private final Set<AccessRecord<MethodCallTarget>> targets = new HashSet<>();

        public AccessSimulator from(JavaMethod method, int lineNumber) {
            return new AccessSimulator(targets, method, lineNumber);
        }

        public AccessSimulator from(Class<?> clazz, String methodName, Class<?>... params) {
            return new AccessSimulator(targets, javaMethod(clazz, methodName, params), 0);
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
            targets.add(new TestAccessRecord<>(targetFrom(target)));
            ClassGraphCreator context = mock(ClassGraphCreator.class);
            when(context.getMethodCallRecordsFor(method)).thenReturn(ImmutableSet.copyOf(targets));
            method.completeFrom(context);
            return getCallToTarget(target);
        }

        public JavaCall<?> to(Class<?> clazz, String methodName, Class<?>... params) {
            return to(javaMethod(clazz, methodName, params));
        }

        private JavaMethodCall getCallToTarget(JavaMethod target) {
            Set<JavaMethodCall> matchingCalls = new HashSet<>();
            for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                if (call.getTarget().equals(targetFrom(target))) {
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

    public static class TestDeclarationHandler implements JavaClassProcessor.DeclarationHandler, ImportContext {
        private Set<JavaField.Builder> fieldBuilders = new HashSet<>();
        private Set<JavaMethod.Builder> methodBuilders = new HashSet<>();
        private Set<JavaConstructor.Builder> constructorBuilders = new HashSet<>();
        private Optional<JavaStaticInitializer.Builder> staticInitializerBuilder = Optional.absent();
        private Set<JavaAnnotation.Builder> annotations = new HashSet<>();

        @Override
        public void onNewClass(String className) {

        }

        @Override
        public void onDeclaredField(JavaField.Builder fieldBuilder) {
            fieldBuilders.add(fieldBuilder);
        }

        @Override
        public void onDeclaredConstructor(JavaConstructor.Builder builder) {
            constructorBuilders.add(builder);
        }

        @Override
        public void onDeclaredMethod(JavaMethod.Builder builder) {
            methodBuilders.add(builder);
        }

        @Override
        public void onDeclaredStaticInitializer(JavaStaticInitializer.Builder builder) {
            staticInitializerBuilder = Optional.of(builder);
        }

        @Override
        public void onDeclaredAnnotations(Set<JavaAnnotation.Builder> annotations) {
            this.annotations = annotations;
        }

        @Override
        public Set<FieldAccessRecord> getFieldAccessRecordsFor(JavaCodeUnit codeUnit) {
            return ImmutableSet.of();
        }

        @Override
        public Set<AccessRecord<MethodCallTarget>> getMethodCallRecordsFor(JavaCodeUnit codeUnit) {
            return ImmutableSet.of();
        }

        @Override
        public Set<AccessRecord<ConstructorCallTarget>> getConstructorCallRecordsFor(JavaCodeUnit codeUnit) {
            return ImmutableSet.of();
        }

        @Override
        public JavaClass getJavaClassWithType(String name) {
            return importSingle(classForName(name));
        }

        @Override
        public Set<JavaField> createFields(JavaClass owner) {
            return build(fieldBuilders, owner);
        }

        @Override
        public Set<JavaMethod> createMethods(JavaClass owner) {
            return build(methodBuilders, owner);
        }

        @Override
        public Set<JavaConstructor> createConstructors(JavaClass owner) {
            return build(constructorBuilders, owner);
        }

        @Override
        public Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner) {
            return staticInitializerBuilder.isPresent() ?
                    Optional.of(staticInitializerBuilder.get().build(owner)) :
                    Optional.<JavaStaticInitializer>absent();
        }

        @Override
        public Map<String, JavaAnnotation> createAnnotations(JavaClass owner) {
            return buildAnnotations(annotations);
        }
    }
}