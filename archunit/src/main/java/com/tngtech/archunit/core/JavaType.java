package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tngtech.archunit.core.ArchUnitException.ReflectionException;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;
import static com.tngtech.archunit.core.Formatters.ensureSimpleName;

interface JavaType {
    String getName();

    String getSimpleName();

    String getPackage();

    @ResolvesTypesViaReflection
    Class<?> resolveClass();

    @ResolvesTypesViaReflection
    Class<?> resolveClass(ClassLoader classLoader);

    @ResolvesTypesViaReflection
    Optional<Class<?>> tryResolveClass();

    class From {
        private static final ImmutableMap<String, Class<?>> primitiveClassesByName =
                Maps.uniqueIndex(allPrimitiveTypes(), new Function<Class<?>, String>() {
                    @Override
                    public String apply(Class<?> input) {
                        return input.getName();
                    }
                });
        private static final ImmutableBiMap<String, Class<?>> primitiveClassesByDescriptor =
                ImmutableBiMap.copyOf(Maps.uniqueIndex(allPrimitiveTypes(), new Function<Class<?>, String>() {
                    @Override
                    public String apply(Class<?> input) {
                        return Type.getType(input).getDescriptor();
                    }
                }));
        private static final Map<String, Class<?>> primitiveClassesByNameOrDescriptor =
                ImmutableMap.<String, Class<?>>builder()
                        .putAll(primitiveClassesByName)
                        .putAll(primitiveClassesByDescriptor)
                        .build();

        static JavaType name(String typeName) {
            if (primitiveClassesByNameOrDescriptor.containsKey(typeName)) {
                return new PrimitiveType(Type.getType(primitiveClassesByNameOrDescriptor.get(typeName)).getClassName());
            }
            if (isArray(typeName)) {
                // NOTE: ASM uses the canonical name for arrays (i.e. java.lang.Object[]), but we want the class name,
                //       i.e. [Ljava.lang.Object;
                return new ArrayType(ensureCorrectArrayTypeName(typeName));
            }
            if (typeName.contains("/")) {
                return new ObjectType(Type.getType(typeName).getClassName());
            }
            return new ObjectType(typeName);
        }

        private static boolean isArray(String typeName) {
            return typeName.startsWith("[") || typeName.endsWith("]"); // We support class name ([Ljava.lang.Object;) and canonical name java.lang.Object[]
        }

        private static String ensureCorrectArrayTypeName(String name) {
            return name.endsWith("[]") ? convertCanonicalArrayNameToClassName(name) : name;
        }

        private static String convertCanonicalArrayNameToClassName(String name) {
            String arrayDesignator = Strings.repeat("[", CharMatcher.is('[').countIn(name));
            return arrayDesignator + createComponentTypeName(name);
        }

        private static String createComponentTypeName(String name) {
            String baseName = name.replaceAll("(\\[\\])*$", "");

            return primitiveClassesByName.containsKey(baseName) ?
                    createPrimitiveComponentType(baseName) :
                    createObjectComponentType(baseName);
        }

        private static String createPrimitiveComponentType(String componentTypeName) {
            return primitiveClassesByDescriptor.inverse().get(primitiveClassesByName.get(componentTypeName));
        }

        private static String createObjectComponentType(String componentTypeName) {
            return "L" + componentTypeName + ";";
        }

        /**
         * Takes an 'internal' ASM object type name, i.e. the class name but with slashes instead of periods,
         * i.e. java/lang/Object (note that this is not a descriptor like Ljava/lang/Object;)
         */
        static JavaType fromAsmObjectTypeName(String objectTypeName) {
            return asmType(Type.getObjectType(objectTypeName));
        }

        static JavaType asmType(Type type) {
            return name(type.getClassName());
        }

        static JavaType javaClass(JavaClass javaClass) {
            return name(javaClass.getName());
        }

        private static abstract class AbstractType implements JavaType {
            private final String name;
            private final String simpleName;
            private final String javaPackage;

            private AbstractType(String name, String simpleName, String javaPackage) {
                this.name = name;
                this.simpleName = simpleName;
                this.javaPackage = javaPackage;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSimpleName() {
                return simpleName;
            }

            @Override
            public String getPackage() {
                return javaPackage;
            }

            @Override
            public Class<?> resolveClass() {
                return resolveClass(getClass().getClassLoader());
            }

            @Override
            public Class<?> resolveClass(ClassLoader classLoader) {
                try {
                    return classForName(classLoader);
                } catch (ClassNotFoundException e) {
                    throw new ReflectionException(e);
                }
            }

            @Override
            public Optional<Class<?>> tryResolveClass() {
                try {
                    return Optional.<Class<?>>of(classForName(getClass().getClassLoader()));
                } catch (ClassNotFoundException e) {
                    return Optional.absent();
                }
            }

            @MayResolveTypesViaReflection(reason = "This method is one of the known sources for resolving via reflection")
            Class<?> classForName(ClassLoader classLoader) throws ClassNotFoundException {
                return Class.forName(getName(), false, classLoader);
            }

            @Override
            public int hashCode() {
                return Objects.hash(getName());
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null || getClass() != obj.getClass()) {
                    return false;
                }
                final JavaType other = (JavaType) obj;
                return Objects.equals(this.getName(), other.getName());
            }

            @Override
            public String toString() {
                return getClass().getSimpleName() + "{" + getName() + "}";
            }
        }

        private static class ObjectType extends AbstractType {
            static final String CLASS_NAME_REGEX = "(\\w+\\.)*(\\w|\\$)+";

            ObjectType(String fullName) {
                super(fullName, ensureSimpleName(fullName), createPackage(fullName));
                checkArgument(fullName.matches(CLASS_NAME_REGEX), "Full name %s is invalid", fullName);
            }

            private static String createPackage(String fullName) {
                return fullName.replaceAll("\\.?[^.]*$", "");
            }
        }

        private static class PrimitiveType extends AbstractType {
            PrimitiveType(String fullName) {
                super(fullName, fullName, "");
                checkArgument(primitiveClassesByName.containsKey(fullName), "'%s' must be a primitive name", fullName);
            }

            @Override
            Class<?> classForName(ClassLoader classLoader) throws ClassNotFoundException {
                return primitiveClassesByName.get(getName());
            }
        }

        private static class ArrayType extends AbstractType {
            private static final String PRIMITIVE_COMPONENT_TYPE_REGEX = Joiner.on("|").join(primitiveClassesByDescriptor.keySet());
            private static final String OBJECT_COMPONENT_TYPE_REGEX = "(L" + ObjectType.CLASS_NAME_REGEX + ";)";
            private static final String COMPONENT_TYPE_REGEX = PRIMITIVE_COMPONENT_TYPE_REGEX + "|" + OBJECT_COMPONENT_TYPE_REGEX;
            private static final String ARRAY_REGEX = "\\[+(" + COMPONENT_TYPE_REGEX + ")";
            private static final Pattern ARRAY_PATTERN = Pattern.compile(ARRAY_REGEX);

            ArrayType(String fullName) {
                super(fullName, createSimpleName(fullName), "");
            }

            private static String createSimpleName(String fullName) {
                checkArgument(ARRAY_PATTERN.matcher(fullName).matches(), "'%s' must be an array name", fullName);
                // NOTE: ASM type.getClassName() returns the canonical name for any array, e.g. java.lang.Object[]
                return ensureSimpleName(Type.getType(fullName).getClassName());
            }
        }
    }
}
