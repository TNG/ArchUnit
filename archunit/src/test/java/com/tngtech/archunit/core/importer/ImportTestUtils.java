package com.tngtech.archunit.core.importer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext;
import com.tngtech.archunit.core.domain.ImportContext;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaConstructorReference;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaMethodReference;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.domain.TypeCast;
import com.tngtech.archunit.core.importer.DomainBuilders.BuilderWithBuildParameter;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder.ValueBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TryCatchBlockBuilder;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;
import org.objectweb.asm.Type;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.importer.DomainBuilders.newConstructorCallTargetBuilder;
import static com.tngtech.archunit.core.importer.DomainBuilders.newMethodCallTargetBuilder;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class ImportTestUtils {

    private static Set<JavaConstructor> createConstructors(JavaClass owner, Class<?> inputClass, ImportedClasses importedClasses) {
        return finish(constructorBuildersFor(inputClass), owner, importedClasses);
    }

    private static Set<JavaMethod> createMethods(JavaClass owner, Class<?> inputClass, ImportedClasses importedClasses) {
        return finish(methodBuildersFor(inputClass), owner, importedClasses);
    }

    private static Set<JavaField> createFields(JavaClass owner, Class<?> inputClass, ImportedClasses importedClasses) {
        return finish(fieldBuildersFor(inputClass), owner, importedClasses);
    }

    private static Set<BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuildersFor(Class<?> inputClass) {
        return stream(inputClass.getDeclaredFields())
                .map(field -> new DomainBuilders.JavaFieldBuilder()
                        .withName(field.getName())
                        .withDescriptor(Type.getDescriptor(field.getType()))
                        .withModifiers(JavaModifier.getModifiersForField(field.getModifiers()))
                        .withType(Optional.empty(), JavaClassDescriptor.From.name(field.getType().getName())))
                .collect(toSet());
    }

    private static Set<BuilderWithBuildParameter<JavaClass, JavaMethod>> methodBuildersFor(Class<?> inputClass) {
        return stream(inputClass.getDeclaredMethods())
                .map(method -> new DomainBuilders.JavaMethodBuilder()
                        .withReturnType(
                                Optional.empty(),
                                JavaClassDescriptor.From.name(method.getReturnType().getName()))
                        .withParameterTypes(Collections.emptyList(), typesFrom(method.getParameterTypes()))
                        .withName(method.getName())
                        .withDescriptor(Type.getMethodDescriptor(method))
                        .withModifiers(JavaModifier.getModifiersForMethod(method.getModifiers()))
                        .withThrowsClause(typesFrom(method.getExceptionTypes())))
                .collect(toSet());
    }

    private static Set<BuilderWithBuildParameter<JavaClass, JavaConstructor>> constructorBuildersFor(Class<?> inputClass) {
        return stream(inputClass.getDeclaredConstructors())
                .map(constructor -> new DomainBuilders.JavaConstructorBuilder()
                        .withReturnType(
                                Optional.empty(),
                                JavaClassDescriptor.From.name(void.class.getName()))
                        .withParameterTypes(Collections.emptyList(), typesFrom(constructor.getParameterTypes()))
                        .withName(CONSTRUCTOR_NAME)
                        .withDescriptor(Type.getConstructorDescriptor(constructor))
                        .withModifiers(JavaModifier.getModifiersForMethod(constructor.getModifiers()))
                        .withThrowsClause(typesFrom(constructor.getExceptionTypes())))
                .collect(toSet());
    }

    private static <T> Set<T> finish(Set<BuilderWithBuildParameter<JavaClass, T>> builders, JavaClass owner,
            ImportedClasses importedClasses) {
        return builders.stream().map(b -> b.build(owner, importedClasses)).collect(toImmutableSet());
    }

    private static JavaClass javaClassFor(Class<?> owner) {
        return new DomainBuilders.JavaClassBuilder()
                .withDescriptor(JavaClassDescriptor.From.name(owner.getName()))
                .withInterface(owner.isInterface())
                .withModifiers(JavaModifier.getModifiersForClass(owner.getModifiers()))
                .build();
    }

    private static Map<String, Object> mapOf(Annotation annotation, Class<?> annotatedClass, ImportContext importContext) {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            result.put(method.getName(), get(annotation, annotatedClass, method.getName(), importContext));
        }
        return result.build();
    }

    private static Object get(Annotation annotation, Class<?> owner, String methodName, ImportContext importContext) {
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            method.setAccessible(true);
            Object result = method.invoke(annotation);
            if (result instanceof Class) {
                return importContext.resolveClass(((Class<?>) result).getName());
            }
            if (result instanceof Class[]) {
                List<JavaClass> classes = javaClassesFrom((Class<?>[]) result, importContext);
                return classes.toArray(new JavaClass[0]);
            }
            if (result instanceof Enum<?>) {
                return ImportTestUtils.enumConstant((Enum<?>) result);
            }
            if (result instanceof Enum[]) {
                return enumConstants((Enum<?>[]) result);
            }
            if (result instanceof Annotation) {
                return javaAnnotationFrom((Annotation) result, owner);
            }
            if (result instanceof Annotation[]) {
                return javaAnnotationsFrom((Annotation[]) result, owner).toArray(new JavaAnnotation<?>[0]);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<JavaClass> javaClassesFrom(Class<?>[] classes, ImportContext importContext) {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        for (Class<?> c : classes) {
            result.add(importContext.resolveClass(c.getName()));
        }
        return result.build();
    }

    private static JavaEnumConstant[] enumConstants(Enum<?>[] enums) {
        return stream(enums).map(ImportTestUtils::enumConstant).toArray(JavaEnumConstant[]::new);
    }

    private static List<? extends JavaAnnotation<?>> javaAnnotationsFrom(Annotation[] annotations, Class<?> owner) {
        return javaAnnotationsFrom(annotations, simulateImportContext(owner, simpleImportedClasses()), owner);
    }

    private static List<JavaAnnotation<JavaClass>> javaAnnotationsFrom(Annotation[] annotations, ImportContext importContext, Class<?> owner) {
        return stream(annotations).map(a -> javaAnnotationFrom(a, owner, importContext)).collect(toList());
    }

    public static JavaMethodCall newMethodCall(JavaMethod origin, MethodCallTarget target, int lineNumber) {
        return newMethodCallBuilder(origin, target, lineNumber).build();
    }

    public static JavaMethodCallBuilder newMethodCallBuilder(JavaMethod origin, MethodCallTarget target, int lineNumber) {
        return new JavaMethodCallBuilder()
                .withOrigin(origin)
                .withTarget(target)
                .withLineNumber(lineNumber);
    }

    public static JavaFieldAccess newFieldAccess(JavaMethod origin, JavaField target, int lineNumber, JavaFieldAccess.AccessType accessType) {
        return new DomainBuilders.JavaFieldAccessBuilder()
                .withOrigin(origin)
                .withTarget(targetFrom(target))
                .withLineNumber(lineNumber)
                .withAccessType(accessType)
                .build();
    }

    private static List<JavaClassDescriptor> typesFrom(Class<?>[] classes) {
        return stream(classes).map(clazz -> JavaClassDescriptor.From.name(clazz.getName())).collect(toList());
    }

    private static <E extends Enum<?>> JavaEnumConstant enumConstant(E value) {
        return new DomainBuilders.JavaEnumConstantBuilder()
                .withDeclaringClass(simulateImport(value.getDeclaringClass(), simpleImportedClasses()))
                .withName(value.name())
                .build();
    }

    public static AccessTarget.ConstructorCallTarget targetFrom(JavaConstructor target) {
        return newConstructorCallTargetBuilder()
                .withOwner(target.getOwner())
                .withParameters(target.getRawParameterTypes())
                .withReturnType(target.getRawReturnType())
                .withMember(() -> Optional.of(target))
                .build();
    }

    public static AccessTarget.FieldAccessTarget targetFrom(JavaField field) {
        return new FieldAccessTargetBuilder()
                .withOwner(field.getOwner())
                .withName(field.getName())
                .withType(field.getRawType())
                .withMember(() -> Optional.of(field))
                .build();
    }

    public static MethodCallTarget targetFrom(JavaMethod target, Supplier<Optional<JavaMethod>> resolveSupplier) {
        return newMethodCallTargetBuilder()
                .withOwner(target.getOwner())
                .withName(target.getName())
                .withParameters(target.getRawParameterTypes())
                .withReturnType(target.getRawReturnType())
                .withMember(resolveSupplier)
                .build();
    }

    public static JavaAnnotation<JavaClass> javaAnnotationFrom(Annotation annotation, Class<?> annotatedClass) {
        return javaAnnotationFrom(annotation, annotatedClass, simulateImportContext(annotatedClass, ImportTestUtils.simpleImportedClasses()));
    }

    private static JavaAnnotation<JavaClass> javaAnnotationFrom(Annotation annotation, Class<?> annotatedClass, ImportContext importContext) {
        return javaAnnotationBuilderFrom(annotation, annotatedClass, importContext)
                .build(importContext.resolveClass(annotatedClass.getName()), ImportTestUtils.simpleImportedClasses());
    }

    private static DomainBuilders.JavaAnnotationBuilder javaAnnotationBuilderFrom(Annotation annotation, Class<?> annotatedClass,
            ImportContext importedClasses) {
        DomainBuilders.JavaAnnotationBuilder builder = new DomainBuilders.JavaAnnotationBuilder()
                .withType(JavaClassDescriptor.From.name(annotation.annotationType().getName()));
        for (Map.Entry<String, Object> entry : mapOf(annotation, annotatedClass, importedClasses).entrySet()) {
            builder.addProperty(entry.getKey(), ValueBuilder.fromPrimitiveProperty(entry.getValue()));
        }
        return builder;
    }

    public static ImportedTestClasses simpleImportedClasses() {
        return new ImportedTestClasses();
    }

    private static JavaClass simulateImport(Class<?> owner, ImportedTestClasses importedClasses) {
        JavaClass javaClass = ImportTestUtils.javaClassFor(owner);
        importedClasses.register(javaClass);
        ImportContext context = simulateImportContext(owner, importedClasses);
        DomainObjectCreationContext.completeMembers(javaClass, context);
        return javaClass;
    }

    private static ImportContext simulateImportContext(Class<?> inputClass, ImportedTestClasses importedClasses) {
        return new ImportContextStub() {
            @Override
            public Set<JavaField> createFields(JavaClass owner) {
                return ImportTestUtils.createFields(owner, inputClass, importedClasses);
            }

            @Override
            public Set<JavaMethod> createMethods(JavaClass owner) {
                return ImportTestUtils.createMethods(owner, inputClass, importedClasses);
            }

            @Override
            public Set<JavaConstructor> createConstructors(JavaClass owner) {
                return ImportTestUtils.createConstructors(owner, inputClass, importedClasses);
            }

            @Override
            public Map<String, JavaAnnotation<JavaClass>> createAnnotations(JavaClass owner) {
                return annotationsFor(inputClass, simulateImportContext(inputClass, importedClasses));
            }
        };
    }

    private static Map<String, JavaAnnotation<JavaClass>> annotationsFor(Class<?> inputClass, ImportContext importedClasses) {
        return javaAnnotationsFrom(inputClass.getAnnotations(), importedClasses, inputClass).stream()
                .collect(toMap(annotation -> annotation.getRawType().getName(), identity()));
    }

    public static class ImportedTestClasses extends ImportedClasses {
        private final Map<String, JavaClass> imported = new HashMap<>();

        ImportedTestClasses() {
            super(
                    emptyMap(),
                    new ClassResolver() {
                        @Override
                        public void setClassUriImporter(ClassUriImporter classUriImporter) {
                        }

                        @Override
                        public Optional<JavaClass> tryResolve(String typeName) {
                            return Optional.empty();
                        }
                    },
                    (declaringClassName, methodName) -> Optional.empty());
        }

        void register(JavaClass clazz) {
            imported.put(clazz.getName(), clazz);
        }

        @Override
        public JavaClass getOrResolve(String typeName) {
            return imported.containsKey(typeName) ?
                    imported.get(typeName) :
                    importNew(JavaClassDescriptor.From.name(typeName).resolveClass());
        }

        private JavaClass importNew(Class<?> owner) {
            JavaClass result = javaClassFor(owner);
            imported.put(result.getName(), result);
            return result;
        }
    }

    private static class ImportContextStub implements ImportContext {

        @Override
        public Optional<JavaClass> createSuperclass(JavaClass owner) {
            return Optional.empty();
        }

        @Override
        public Optional<JavaType> createGenericSuperclass(JavaClass owner) {
            return Optional.empty();
        }

        @Override
        public Optional<List<JavaType>> createGenericInterfaces(JavaClass owner) {
            return Optional.empty();
        }

        @Override
        public List<JavaClass> createInterfaces(JavaClass owner) {
            return Collections.emptyList();
        }

        @Override
        public List<JavaTypeVariable<JavaClass>> createTypeParameters(JavaClass owner) {
            return Collections.emptyList();
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
            return Optional.empty();
        }

        @Override
        public Map<String, JavaAnnotation<JavaClass>> createAnnotations(JavaClass owner) {
            return emptyMap();
        }

        @Override
        public Map<String, JavaAnnotation<JavaMember>> createAnnotations(JavaMember owner) {
            return emptyMap();
        }

        @Override
        public Optional<JavaClass> createEnclosingClass(JavaClass owner) {
            return Optional.empty();
        }

        @Override
        public Optional<JavaCodeUnit> createEnclosingCodeUnit(JavaClass owner) {
            return Optional.empty();
        }

        @Override
        public Set<JavaFieldAccess> createFieldAccessesFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaMethodCall> createMethodCallsFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaConstructorCall> createConstructorCallsFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaMethodReference> createMethodReferencesFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaConstructorReference> createConstructorReferencesFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
            return Collections.emptySet();
        }

        @Override
        public Set<TryCatchBlockBuilder> createTryCatchBlockBuilders(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public Set<ReferencedClassObject> createReferencedClassObjectsFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public Set<InstanceofCheck> createInstanceofChecksFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public Set<TypeCast> createTypeCastsFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public JavaClass resolveClass(String fullyQualifiedClassName) {
            throw new UnsupportedOperationException("Override me where necessary");
        }
    }
}
