/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.domain;

import java.util.Map;
import java.util.Objects;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;

@Internal
public interface JavaType {
    String getName();

    String getSimpleName();

    String getPackageName();

    @ResolvesTypesViaReflection
    Class<?> resolveClass();

    @ResolvesTypesViaReflection
    Class<?> resolveClass(ClassLoader classLoader);

    Optional<JavaType> tryGetComponentType();

    boolean isPrimitive();

    boolean isArray();

    JavaType withSimpleName(String simpleName);

    @Internal
    final class From {
        private static final LoadingCache<String, JavaType> typeCache = CacheBuilder.newBuilder().build(new CacheLoader<String, JavaType>() {
            @Override
            public JavaType load(String typeName) {
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
        });
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

        public static JavaType name(String typeName) {
            return typeCache.getUnchecked(typeName);
        }

        private static boolean isArray(String typeName) {
            // We support class name ([Ljava.lang.Object;) and canonical name java.lang.Object[]
            return typeName.startsWith("[") || typeName.endsWith("]");
        }

        private static String ensureCorrectArrayTypeName(String name) {
            return name.endsWith("[]") ? convertCanonicalArrayNameToClassName(name) : name;
        }

        private static String convertCanonicalArrayNameToClassName(String name) {
            String arrayDesignator = Strings.repeat("[", CharMatcher.is('[').countIn(name));
            return arrayDesignator + createComponentTypeName(name);
        }

        private static String createComponentTypeName(String name) {
            String baseName = name.substring(0, name.indexOf("[]"));

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

        static JavaType javaClass(JavaClass javaClass) {
            return name(javaClass.getName());
        }

        private abstract static class AbstractType implements JavaType {
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
            public String getPackageName() {
                return javaPackage;
            }

            @Override
            public Class<?> resolveClass() {
                return resolveClass(getCurrentClassLoader(getClass()));
            }

            @Override
            public Class<?> resolveClass(ClassLoader classLoader) {
                try {
                    return classForName(classLoader);
                } catch (ClassNotFoundException e) {
                    throw new ReflectionException(e);
                }
            }

            @MayResolveTypesViaReflection(reason = "This method is one of the known sources for resolving via reflection")
            Class<?> classForName(ClassLoader classLoader) throws ClassNotFoundException {
                return Class.forName(getName(), false, classLoader);
            }

            @Override
            public Optional<JavaType> tryGetComponentType() {
                return Optional.absent();
            }

            @Override
            public boolean isPrimitive() {
                return false;
            }

            @Override
            public boolean isArray() {
                return false;
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
            ObjectType(String fullName) {
                this(fullName, ensureSimpleName(fullName), createPackage(fullName));
            }

            private ObjectType(String fullName, String simpleName, String packageName) {
                super(fullName, simpleName, packageName);
            }

            @Override
            public JavaType withSimpleName(String simpleName) {
                return new ObjectType(getName(), simpleName, getPackageName());
            }
        }

        private static String createPackage(String fullName) {
            int packageEnd = fullName.lastIndexOf('.');
            return packageEnd >= 0 ? fullName.substring(0, packageEnd) : "";
        }

        private static class PrimitiveType extends AbstractType {
            PrimitiveType(String fullName) {
                super(fullName, fullName, "");
                checkArgument(primitiveClassesByName.containsKey(fullName), "'%s' must be a primitive name", fullName);
            }

            @Override
            Class<?> classForName(ClassLoader classLoader) {
                return primitiveClassesByName.get(getName());
            }

            @Override
            public boolean isPrimitive() {
                return true;
            }

            @Override
            public JavaType withSimpleName(String simpleName) {
                throw new UnsupportedOperationException("It should never make sense to override the simple type of a primitive");
            }
        }

        private static class ArrayType extends AbstractType {
            ArrayType(String fullName) {
                this(fullName, createSimpleName(fullName), createPackageOfComponentType(fullName));
            }

            private ArrayType(String fullName, String simpleName, String packageName) {
                super(fullName, simpleName, packageName);
            }

            private static String createPackageOfComponentType(String fullName) {
                String componentType = getCanonicalName(fullName).replace("[]", "");
                return createPackage(componentType);
            }

            private static String createSimpleName(String fullName) {
                return ensureSimpleName(getCanonicalName(fullName));
            }

            private static String getCanonicalName(String fullName) {
                return Type.getType(fullName).getClassName();
            }

            @Override
            public boolean isArray() {
                return true;
            }

            @Override
            public JavaType withSimpleName(String simpleName) {
                return new ArrayType(getName(), simpleName, getPackageName());
            }

            @Override
            public Optional<JavaType> tryGetComponentType() {
                String canonicalName = getCanonicalName(getName());
                String componentTypeName = canonicalName.substring(0, canonicalName.lastIndexOf("["));
                return Optional.of(JavaType.From.name(componentTypeName));
            }
        }
    }
}
