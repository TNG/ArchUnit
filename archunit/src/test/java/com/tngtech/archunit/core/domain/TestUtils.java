package com.tngtech.archunit.core.domain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.Source.Md5sum;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.ImportTestUtils;
import com.tngtech.archunit.core.importer.ImportTestUtils.ImportedTestClasses;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.importer.ImportTestUtils.newFieldAccess;
import static com.tngtech.archunit.core.importer.ImportTestUtils.newMethodCall;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.getHierarchy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    public static final Md5sum MD5_SUM_DISABLED = Md5sum.DISABLED;

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
        return ImportTestUtils.javaMethodViaReflection(clazz, method);
    }

    public static JavaClassList javaClassList(Class<?>... types) {
        List<JavaClass> classes = new ArrayList<>();
        for (Class<?> type : types) {
            classes.add(javaClassViaReflection(type));
        }
        return new JavaClassList(classes);
    }

    public static JavaClasses importClasses(Class<?>... classes) {
        return new ClassFileImporter().importClasses(classes);
    }

    public static JavaClasses importHierarchies(Class<?>... classes) {
        Set<Class<?>> hierarchies = new HashSet<>();
        for (Class<?> clazz : classes) {
            hierarchies.addAll(getHierarchy(clazz));
        }
        return new ClassFileImporter().importClasses(hierarchies);
    }

    public static ImportedContext withinImportedClasses(Class<?>... contextClasses) {
        return new ImportedContext(importClasses(contextClasses));
    }

    public static Md5sum md5sumOf(byte[] bytes) {
        return Md5sum.of(bytes);
    }

    public static JavaClass javaClassViaReflection(Class<?> owner) {
        return getOnlyElement(javaClassesViaReflection(owner));
    }

    public static JavaField javaFieldViaReflection(Class<?> owner, String name) {
        return javaFieldViaReflection(javaClassViaReflection(owner), name);
    }

    public static JavaField javaFieldViaReflection(JavaClass owner, String name) {
        try {
            Field field = owner.reflect().getDeclaredField(name);
            return ImportTestUtils.javaFieldViaReflection(field, owner);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaClasses javaClassesViaReflection(Class<?>... classes) {
        final ImportedTestClasses importedClasses = ImportTestUtils.simpleImportedClasses();
        final Map<String, JavaClass> result = new HashMap<>();
        for (Class<?> aClass : classes) {
            JavaClass newClass = ImportTestUtils.simulateImport(aClass, importedClasses);
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

    public static JavaMethodCallBuilder newMethodCallBuilder(JavaMethod origin, MethodCallTarget target, int lineNumber) {
        return ImportTestUtils.newMethodCallBuilder(origin, target, lineNumber);
    }

    public static AccessesSimulator simulateCall() {
        return new AccessesSimulator();
    }

    public static DescribedPredicate<Object> predicateWithDescription(String description) {
        return DescribedPredicate.alwaysTrue().as(description);
    }

    public static MethodCallTarget resolvedTargetFrom(JavaMethod target) {
        return ImportTestUtils.targetFrom(target, Suppliers.ofInstance(Collections.singleton(target)));
    }

    static MethodCallTarget unresolvedTargetFrom(JavaMethod target) {
        return ImportTestUtils.targetFrom(target, Suppliers.ofInstance(Collections.<JavaMethod>emptySet()));
    }

    public static Class<?>[] asClasses(List<JavaClass> parameters) {
        List<Class<?>> result = new ArrayList<>();
        for (JavaClass javaClass : parameters) {
            result.add(javaClass.reflect());
        }
        return result.toArray(new Class[result.size()]);
    }

    public static Class<?> classForName(String name) {
        return JavaType.From.name(name).resolveClass();
    }

    static ImportedTestClasses simpleImportedClasses() {
        return ImportTestUtils.simpleImportedClasses();
    }

    static JavaAnnotation javaAnnotationFrom(Annotation annotation) {
        return ImportTestUtils.javaAnnotationFrom(annotation);
    }

    public static FieldAccessTarget targetFrom(JavaField javaField) {
        return ImportTestUtils.targetFrom(javaField);
    }

    public static ConstructorCallTarget targetFrom(JavaConstructor constructor) {
        return ImportTestUtils.targetFrom(constructor);
    }

    public static Dependency dependencyFrom(JavaAccess<?> access) {
        return Dependency.from(access);
    }

    public static class AccessesSimulator {
        private final Set<MethodCallTarget> targets = new HashSet<>();

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
        private final Set<MethodCallTarget> targets;
        private final JavaMethod method;
        private final int lineNumber;

        private AccessSimulator(Set<MethodCallTarget> targets, JavaMethod method, int lineNumber) {
            this.targets = targets;
            this.method = method;
            this.lineNumber = lineNumber;
        }

        public JavaMethodCall to(JavaMethod target) {
            return to(resolvedTargetFrom(target));
        }

        private JavaMethodCall to(MethodCallTarget methodCallTarget) {
            targets.add(methodCallTarget);
            ImportContext context = mock(ImportContext.class);
            Set<JavaMethodCall> calls = new HashSet<>();
            for (MethodCallTarget target : targets) {
                calls.add(newMethodCall(method, target, lineNumber));
            }
            when(context.getMethodCallsFor(method)).thenReturn(ImmutableSet.copyOf(calls));
            method.completeFrom(context);
            return getCallToTarget(methodCallTarget);
        }

        public JavaMethodCall to(Class<?> clazz, String methodName, Class<?>... params) {
            return to(resolvedTargetFrom(javaMethodViaReflection(clazz, methodName, params)));
        }

        public JavaMethodCall toUnresolved(Class<?> clazz, String methodName, Class<?>... params) {
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
            ImportContext context = mock(ImportContext.class);
            when(context.getFieldAccessesFor(method))
                    .thenReturn(ImmutableSet.of(
                            newFieldAccess(method, target, lineNumber, accessType)
                    ));
            method.completeFrom(context);
        }
    }

    public static class ImportedContext {
        private final JavaClasses classes;

        private ImportedContext(JavaClasses classes) {
            this.classes = classes;
        }

        public CallRetriever getCallFrom(Class<?> originClass, String codeUnitName, Class<?>... paramTypes) {
            JavaClass owner = classes.get(originClass);
            return new CallRetriever(owner.getCodeUnitWithParameterTypes(codeUnitName, paramTypes));
        }
    }

    public static class CallRetriever {
        private final JavaCodeUnit codeUnit;

        private CallRetriever(JavaCodeUnit codeUnit) {
            this.codeUnit = codeUnit;
        }

        public JavaConstructorCall toConstructor(Class<?> targetOwner, Class<?>... paramTypes) {
            return findMethod(codeUnit.getConstructorCallsFromSelf(), targetOwner, CONSTRUCTOR_NAME, paramTypes);
        }

        public JavaMethodCall toMethod(Class<?> targetOwner, String methodName, Class<?>... paramTypes) {
            return findMethod(codeUnit.getMethodCallsFromSelf(), targetOwner, methodName, paramTypes);
        }

        private <T extends JavaCall<?>> T findMethod(Set<T> callsFromSelf,
                Class<?> targetOwner,
                String methodName,
                Class<?>[] paramTypes) {

            List<String> paramNames = JavaClass.namesOf(paramTypes);
            for (T call : callsFromSelf) {
                if (call.getTargetOwner().isEquivalentTo(targetOwner) &&
                        call.getTarget().getName().equals(methodName) &&
                        call.getTarget().getParameters().getNames().equals(paramNames)) {
                    return call;
                }
            }
            throw new IllegalStateException(
                    "Couldn't find any call with target " +
                            formatMethod(targetOwner.getName(), methodName, paramNames));
        }
    }
}