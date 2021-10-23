package com.tngtech.archunit.core.importer;

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
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext;
import com.tngtech.archunit.core.domain.ImportContext;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeCreationProcess;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;

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

    private static Set<DomainBuilders.BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuildersFor(Class<?> inputClass) {
        final Set<DomainBuilders.BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuilders = new HashSet<>();
        for (Field field : inputClass.getDeclaredFields()) {
            fieldBuilders.add(new DomainBuilders.JavaFieldBuilder()
                    .withName(field.getName())
                    .withDescriptor(Type.getDescriptor(field.getType()))
                    .withModifiers(JavaModifier.getModifiersForField(field.getModifiers()))
                    .withType(Optional.<JavaTypeCreationProcess<JavaField>>empty(), JavaClassDescriptor.From.name(field.getType().getName())));
        }
        return fieldBuilders;
    }

    private static Set<DomainBuilders.BuilderWithBuildParameter<JavaClass, JavaMethod>> methodBuildersFor(Class<?> inputClass) {
        final Set<DomainBuilders.BuilderWithBuildParameter<JavaClass, JavaMethod>> methodBuilders = new HashSet<>();
        for (Method method : inputClass.getDeclaredMethods()) {
            methodBuilders.add(new DomainBuilders.JavaMethodBuilder()
                    .withReturnType(
                            Optional.<JavaTypeCreationProcess<JavaCodeUnit>>empty(),
                            JavaClassDescriptor.From.name(method.getReturnType().getName()))
                    .withParameterTypes(Collections.<JavaTypeCreationProcess<JavaCodeUnit>>emptyList(), typesFrom(method.getParameterTypes()))
                    .withName(method.getName())
                    .withDescriptor(Type.getMethodDescriptor(method))
                    .withModifiers(JavaModifier.getModifiersForMethod(method.getModifiers()))
                    .withThrowsClause(typesFrom(method.getExceptionTypes())));
        }
        return methodBuilders;
    }

    private static Set<DomainBuilders.BuilderWithBuildParameter<JavaClass, JavaConstructor>> constructorBuildersFor(Class<?> inputClass) {
        final Set<DomainBuilders.BuilderWithBuildParameter<JavaClass, JavaConstructor>> constructorBuilders = new HashSet<>();
        for (Constructor<?> constructor : inputClass.getDeclaredConstructors()) {
            constructorBuilders.add(new DomainBuilders.JavaConstructorBuilder()
                    .withReturnType(
                            Optional.<JavaTypeCreationProcess<JavaCodeUnit>>empty(),
                            JavaClassDescriptor.From.name(void.class.getName()))
                    .withParameterTypes(Collections.<JavaTypeCreationProcess<JavaCodeUnit>>emptyList(), typesFrom(constructor.getParameterTypes()))
                    .withName(CONSTRUCTOR_NAME)
                    .withDescriptor(Type.getConstructorDescriptor(constructor))
                    .withModifiers(JavaModifier.getModifiersForMethod(constructor.getModifiers()))
                    .withThrowsClause(typesFrom(constructor.getExceptionTypes())));
        }
        return constructorBuilders;
    }

    private static <T> Set<T> finish(Set<DomainBuilders.BuilderWithBuildParameter<JavaClass, T>> builders, JavaClass owner,
            ImportedClasses importedClasses) {
        ImmutableSet.Builder<T> result = ImmutableSet.builder();
        for (DomainBuilders.BuilderWithBuildParameter<JavaClass, T> builder : builders) {
            result.add(builder.build(owner, importedClasses));
        }
        return result.build();
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
        List<JavaEnumConstant> result = new ArrayList<>();
        for (Enum<?> e : enums) {
            result.add(ImportTestUtils.enumConstant(e));
        }
        return result.toArray(new JavaEnumConstant[0]);
    }

    private static List<? extends JavaAnnotation<?>> javaAnnotationsFrom(Annotation[] annotations, Class<?> owner) {
        return javaAnnotationsFrom(annotations, simulateImportContext(owner, simpleImportedClasses()), owner);
    }

    private static List<JavaAnnotation<JavaClass>> javaAnnotationsFrom(Annotation[] annotations, ImportContext importContext, Class<?> owner) {
        List<JavaAnnotation<JavaClass>> result = new ArrayList<>();
        for (Annotation a : annotations) {
            result.add(ImportTestUtils.javaAnnotationFrom(a, owner, importContext));
        }
        return result;
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
        List<JavaClassDescriptor> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            result.add(JavaClassDescriptor.From.name(clazz.getName()));
        }
        return result;
    }

    private static <E extends Enum<?>> JavaEnumConstant enumConstant(E value) {
        return new DomainBuilders.JavaEnumConstantBuilder()
                .withDeclaringClass(simulateImport(value.getDeclaringClass(), simpleImportedClasses()))
                .withName(value.name())
                .build();
    }

    public static AccessTarget.ConstructorCallTarget targetFrom(JavaConstructor target) {
        return new DomainBuilders.ConstructorCallTargetBuilder()
                .withOwner(target.getOwner())
                .withParameters(target.getRawParameterTypes())
                .withReturnType(target.getRawReturnType())
                .withConstructor(Suppliers.ofInstance(Optional.of(target)))
                .build();
    }

    public static AccessTarget.FieldAccessTarget targetFrom(JavaField field) {
        return new DomainBuilders.FieldAccessTargetBuilder()
                .withOwner(field.getOwner())
                .withName(field.getName())
                .withType(field.getRawType())
                .withField(Suppliers.ofInstance(Optional.of(field)))
                .build();
    }

    public static MethodCallTarget targetFrom(JavaMethod target, Supplier<Set<JavaMethod>> resolveSupplier) {
        return new DomainBuilders.MethodCallTargetBuilder()
                .withOwner(target.getOwner())
                .withName(target.getName())
                .withParameters(target.getRawParameterTypes())
                .withReturnType(target.getRawReturnType())
                .withMethods(resolveSupplier)
                .build();
    }

    public static JavaAnnotation<JavaClass> javaAnnotationFrom(Annotation annotation, Class<?> annotatedClass) {
        return javaAnnotationFrom(annotation, annotatedClass, simulateImportContext(annotatedClass, ImportTestUtils.simpleImportedClasses()));
    }

    private static JavaAnnotation<JavaClass> javaAnnotationFrom(Annotation annotation, Class<?> annotatedClass, ImportContext importContext) {
        return javaAnnotationBuilderFrom(annotation, annotatedClass, importContext).build(importContext.resolveClass(annotatedClass.getName()), importContext);
    }

    private static DomainBuilders.JavaAnnotationBuilder javaAnnotationBuilderFrom(Annotation annotation, Class<?> annotatedClass,
            ImportContext importedClasses) {
        DomainBuilders.JavaAnnotationBuilder builder = new DomainBuilders.JavaAnnotationBuilder()
                .withType(JavaClassDescriptor.From.name(annotation.annotationType().getName()));
        for (Map.Entry<String, Object> entry : mapOf(annotation, annotatedClass, importedClasses).entrySet()) {
            builder.addProperty(entry.getKey(), DomainBuilders.JavaAnnotationBuilder.ValueBuilder.ofFinished(entry.getValue()));
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

    private static ImportContext simulateImportContext(final Class<?> inputClass, final ImportedTestClasses importedClasses) {
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

    private static ImmutableMap<String, JavaAnnotation<JavaClass>> annotationsFor(Class<?> inputClass, ImportContext importedClasses) {
        return FluentIterable.from(javaAnnotationsFrom(inputClass.getAnnotations(), importedClasses, inputClass))
                .uniqueIndex(new Function<JavaAnnotation<?>, String>() {
                    @Override
                    public String apply(JavaAnnotation<?> input) {
                        return input.getRawType().getName();
                    }
                });
    }

    public static class ImportedTestClasses extends ImportedClasses {
        private final Map<String, JavaClass> imported = new HashMap<>();

        ImportedTestClasses() {
            super(Collections.<String, JavaClass>emptyMap(), new ClassResolver() {
                @Override
                public void setClassUriImporter(ClassUriImporter classUriImporter) {
                }

                @Override
                public Optional<JavaClass> tryResolve(String typeName) {
                    return Optional.empty();
                }
            });
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
        public Optional<Set<JavaType>> createGenericInterfaces(JavaClass owner) {
            return Optional.empty();
        }

        @Override
        public Set<JavaClass> createInterfaces(JavaClass owner) {
            return Collections.emptySet();
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
            return Collections.emptyMap();
        }

        @Override
        public Map<String, JavaAnnotation<JavaMember>> createAnnotations(JavaMember owner) {
            return Collections.emptyMap();
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
        public Set<JavaFieldAccess> createFieldAccessesFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaMethodCall> createMethodCallsFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public Set<JavaConstructorCall> createConstructorCallsFor(JavaCodeUnit codeUnit) {
            return Collections.emptySet();
        }

        @Override
        public JavaClass resolveClass(String fullyQualifiedClassName) {
            throw new UnsupportedOperationException("Override me where necessary");
        }

        @Override
        public Optional<JavaClass> getMethodReturnType(String declaringClassName, String methodName) {
            return Optional.empty();
        }
    }
}
