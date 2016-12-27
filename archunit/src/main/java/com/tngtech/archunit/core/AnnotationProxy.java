package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.core.ReflectionUtils.classForName;

class AnnotationProxy {
    public static <A extends Annotation> A of(Class<A> annotationType, JavaAnnotation toProxy) {
        checkArgument(annotationType.getName().equals(toProxy.getType().getName()),
                "Requested annotation type %s is incompatible with %s of type %s",
                annotationType.getSimpleName(), JavaAnnotation.class.getSimpleName(), toProxy.getType().getSimpleName());

        return newProxy(annotationType, toProxy);
    }

    @SuppressWarnings("unchecked") // annotationType A will be implemented
    private static <A extends Annotation> A newProxy(Class<A> annotationType, JavaAnnotation toProxy) {
        return (A) Proxy.newProxyInstance(
                annotationType.getClassLoader(),
                new Class[]{annotationType},
                new AnnotationMethodInvocationHandler(annotationType, toProxy));
    }

    private static class AnnotationMethodInvocationHandler implements InvocationHandler {
        private final JavaAnnotation toProxy;
        private final Conversions conversions;
        private final Map<MethodKey, SpecificHandler> handlersByMethod;

        private AnnotationMethodInvocationHandler(Class<?> annotationType, JavaAnnotation toProxy) {
            this.toProxy = toProxy;
            conversions = initConversions(annotationType);
            handlersByMethod = initHandlersByMethod(annotationType, toProxy, conversions);
        }

        private Conversions initConversions(Class<?> annotationType) {
            JavaClassConversion javaClassConversion = new JavaClassConversion(annotationType.getClassLoader());
            JavaEnumConstantConversion enumConversion = new JavaEnumConstantConversion();
            JavaAnnotationConversion annotationConversion = new JavaAnnotationConversion(annotationType.getClassLoader());
            return new Conversions(
                    javaClassConversion,
                    new JavaClassArrayConversion(javaClassConversion),
                    enumConversion,
                    new JavaEnumConstantArrayConversion(enumConversion),
                    annotationConversion,
                    new JavaAnnotationArrayConversion(annotationConversion));
        }

        private ImmutableMap<MethodKey, SpecificHandler> initHandlersByMethod(
                Class<?> annotationType, JavaAnnotation toProxy, Conversions conversions) {
            return ImmutableMap.of(
                    new MethodKey("annotationType"), new ConstantReturnValueHandler(annotationType),
                    new MethodKey("equals", Object.class.getName()), new EqualsHandler(),
                    new MethodKey("hashCode"), new HashCodeHandler(),
                    new MethodKey("toString"), new ToStringHandler(annotationType, toProxy, conversions)
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MethodKey key = MethodKey.of(method);
            if (handlersByMethod.containsKey(key)) {
                return handlersByMethod.get(key).handle(proxy, method, args);
            }

            Object result = toProxy.get(method.getName()).or(method.getDefaultValue());
            return conversions.convertIfNecessary(result, method.getReturnType());
        }
    }

    private interface Conversion<F> {
        Object convert(F input, Class<?> returnType);

        boolean canHandle(Object input);
    }

    private static class JavaClassConversion implements Conversion<JavaClass> {
        private final ClassLoader classLoader;

        private JavaClassConversion(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public Class<?> convert(JavaClass input, Class<?> returnType) {
            return classForName(input.getName(), classLoader);
        }

        @Override
        public boolean canHandle(Object input) {
            return JavaClass.class.isInstance(input);
        }
    }

    private static class JavaClassArrayConversion implements Conversion<JavaClass[]> {
        private final JavaClassConversion javaClassConversion;

        private JavaClassArrayConversion(JavaClassConversion javaClassConversion) {
            this.javaClassConversion = javaClassConversion;
        }

        @Override
        public Object convert(JavaClass[] input, Class<?> returnType) {
            return convertArray(input, javaClassConversion, returnType.getComponentType());
        }

        @Override
        public boolean canHandle(Object input) {
            return JavaClass[].class.isInstance(input);
        }
    }

    private static class JavaEnumConstantConversion implements Conversion<JavaEnumConstant> {
        @Override
        @MayResolveTypesViaReflection(reason = "We already depend on the classpath, if we proxy an annotation type")
        public Enum<?> convert(JavaEnumConstant input, Class<?> returnType) {
            for (Object constant : classForName(input.getType().getName()).getEnumConstants()) {
                Enum<?> anEnum = (Enum<?>) constant;
                if (anEnum.name().equals(input.getName())) {
                    return anEnum;
                }
            }
            throw new IllegalStateException(String.format(
                    "Couldn't find Enum Constant %s.%s", input.getType().getSimpleName(), input.getName()));
        }

        @Override
        public boolean canHandle(Object input) {
            return JavaEnumConstant.class.isInstance(input);
        }
    }

    private static class JavaEnumConstantArrayConversion implements Conversion<JavaEnumConstant[]> {
        private final JavaEnumConstantConversion enumConversion;

        private JavaEnumConstantArrayConversion(JavaEnumConstantConversion enumConversion) {
            this.enumConversion = enumConversion;
        }

        @Override
        public Object convert(JavaEnumConstant[] input, Class<?> returnType) {
            return convertArray(input, enumConversion, returnType.getComponentType());
        }

        @Override
        public boolean canHandle(Object input) {
            return JavaEnumConstant[].class.isInstance(input);
        }
    }

    private static class JavaAnnotationConversion implements Conversion<JavaAnnotation> {
        private final ClassLoader classLoader;

        private JavaAnnotationConversion(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public Annotation convert(JavaAnnotation input, Class<?> returnType) {
            return AnnotationProxy.of((Class) classForName(input.getType().getName(), classLoader), input);
        }

        @Override
        public boolean canHandle(Object input) {
            return JavaAnnotation.class.isInstance(input);
        }
    }

    private static class JavaAnnotationArrayConversion implements Conversion<JavaAnnotation[]> {
        private final JavaAnnotationConversion annotationConversion;

        private JavaAnnotationArrayConversion(JavaAnnotationConversion annotationConversion) {
            this.annotationConversion = annotationConversion;
        }

        @Override
        public Object convert(JavaAnnotation[] input, Class<?> returnType) {
            return convertArray(input, annotationConversion, returnType.getComponentType());
        }

        @Override
        public boolean canHandle(Object input) {
            return JavaAnnotation[].class.isInstance(input);
        }
    }

    @SuppressWarnings("unchecked") // Component type = targetType
    private static <F> Object[] convertArray(F[] input, Conversion<F> elementConversion, Class<?> targetType) {
        Object[] result = (Object[]) Array.newInstance(targetType, input.length);
        for (int i = 0; i < input.length; i++) {
            result[i] = elementConversion.convert(input[i], targetType);
        }
        return result;
    }

    private interface SpecificHandler {
        Object handle(Object proxy, Method method, Object[] args);
    }

    private static class ConstantReturnValueHandler implements SpecificHandler {
        private final Object value;

        private ConstantReturnValueHandler(Object value) {
            this.value = value;
        }

        @Override
        public Object handle(Object proxy, Method method, Object[] args) {
            return value;
        }
    }

    private static class EqualsHandler implements SpecificHandler {
        @Override
        public Object handle(Object proxy, Method method, Object[] args) {
            return proxy == args[0];
        }
    }

    private static class HashCodeHandler implements SpecificHandler {
        @Override
        public Object handle(Object proxy, Method method, Object[] args) {
            return System.identityHashCode(proxy);
        }
    }

    private static class ToStringHandler implements SpecificHandler {
        private final Class<?> annotationType;
        private final JavaAnnotation toProxy;
        private final Conversions conversions;

        private ToStringHandler(Class<?> annotationType, JavaAnnotation toProxy, Conversions conversions) {
            this.annotationType = annotationType;
            this.toProxy = toProxy;
            this.conversions = conversions;
        }

        @Override
        public Object handle(Object proxy, Method method, Object[] args) {
            return String.format("@%s(%s)", toProxy.getType().getName(), propertyStrings());
        }

        private String propertyStrings() {
            Set<String> properties = new HashSet<>();
            for (Map.Entry<String, Object> entry : toProxy.getProperties().entrySet()) {
                Class<?> returnType = getDeclaredMethod(entry.getKey()).getReturnType();
                String value = format(conversions.convertIfNecessary(entry.getValue(), returnType));
                properties.add(String.format("%s=%s", entry.getKey(), value));
            }
            return Joiner.on(", ").join(properties);
        }

        private Method getDeclaredMethod(String name) {
            try {
                return annotationType.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        private String format(Object input) {
            if (!input.getClass().isArray()) {
                return "" + input;
            }

            List<String> elemToString = new ArrayList<>();
            for (int i = 0; i < Array.getLength(input); i++) {
                elemToString.add("" + format(Array.get(input, i)));
            }
            return "[" + Joiner.on(", ").join(elemToString) + "]";
        }
    }

    private static class Conversions {
        private final Set<Conversion<?>> conversions;

        private Conversions(Conversion<?>... conversions) {
            this.conversions = ImmutableSet.copyOf(conversions);
        }

        <T> Object convertIfNecessary(T result, Class<?> returnType) {
            return tryFindConversionFor(result).or(new NoOpConversion<T>()).convert(result, returnType);
        }

        private static class NoOpConversion<T> implements Conversion<T> {
            @Override
            public T convert(T input, Class<?> returnType) {
                return input;
            }

            @Override
            public boolean canHandle(Object input) {
                return true;
            }
        }

        @SuppressWarnings("unchecked") // Trust sanity of canHandle(..)
        private <F> Optional<Conversion<F>> tryFindConversionFor(F result) {
            for (Conversion<?> conversion : conversions) {
                if (conversion.canHandle(result)) {
                    return Optional.of((Conversion<F>) conversion);
                }
            }
            return Optional.absent();
        }
    }

    private static class MethodKey {
        private final String name;
        private final List<String> paramTypeNames;

        private MethodKey(String name, String... paramTypeNames) {
            this(name, ImmutableList.copyOf(paramTypeNames));
        }

        private MethodKey(String name, ImmutableList<String> paramTypeNames) {
            this.name = name;
            this.paramTypeNames = paramTypeNames;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, paramTypeNames);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MethodKey other = (MethodKey) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.paramTypeNames, other.paramTypeNames);
        }

        public static MethodKey of(Method method) {
            ImmutableList.Builder<String> paramTypeNames = ImmutableList.builder();
            for (Class<?> type : method.getParameterTypes()) {
                paramTypeNames.add(type.getName());
            }
            return new MethodKey(method.getName(), paramTypeNames.build());
        }
    }
}
