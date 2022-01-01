/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
public interface JavaClassDescriptor {
    String getFullyQualifiedClassName();

    String getSimpleClassName();

    String getPackageName();

    @ResolvesTypesViaReflection
    Class<?> resolveClass();

    @ResolvesTypesViaReflection
    Class<?> resolveClass(ClassLoader classLoader);

    Optional<JavaClassDescriptor> tryGetComponentType();

    boolean isPrimitive();

    boolean isArray();

    JavaClassDescriptor withSimpleClassName(String simpleName);

    JavaClassDescriptor toArrayDescriptor();

    @Internal
    final class From {
        private static final LoadingCache<String, JavaClassDescriptor> descriptorCache =
                CacheBuilder.newBuilder().build(new CacheLoader<String, JavaClassDescriptor>() {
                    @Override
                    public JavaClassDescriptor load(String typeName) {
                        if (primitiveClassesByNameOrDescriptor.containsKey(typeName)) {
                            return new PrimitiveClassDescriptor(Type.getType(primitiveClassesByNameOrDescriptor.get(typeName)).getClassName());
                        }
                        if (isArray(typeName)) {
                            // NOTE: ASM uses the canonical name for arrays (i.e. java.lang.Object[]), but we want the class name,
                            //       i.e. [Ljava.lang.Object;
                            return new ArrayClassDescriptor(ensureCorrectArrayTypeName(typeName));
                        }
                        return new ObjectClassDescriptor(typeName);
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

        public static JavaClassDescriptor name(String typeName) {
            return descriptorCache.getUnchecked(typeName);
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

        public static JavaClassDescriptor javaClass(JavaClass javaClass) {
            return name(javaClass.getName());
        }

        private abstract static class AbstractClassDescriptor implements JavaClassDescriptor {
            private final String name;
            private final String simpleName;
            private final String javaPackage;

            private AbstractClassDescriptor(String name, String simpleName, String javaPackage) {
                this.name = name;
                this.simpleName = simpleName;
                this.javaPackage = javaPackage;
            }

            @Override
            public String getFullyQualifiedClassName() {
                return name;
            }

            @Override
            public String getSimpleClassName() {
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
                return Class.forName(getFullyQualifiedClassName(), false, classLoader);
            }

            @Override
            public Optional<JavaClassDescriptor> tryGetComponentType() {
                return Optional.empty();
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
                return Objects.hash(getFullyQualifiedClassName());
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null || getClass() != obj.getClass()) {
                    return false;
                }
                final JavaClassDescriptor other = (JavaClassDescriptor) obj;
                return Objects.equals(this.getFullyQualifiedClassName(), other.getFullyQualifiedClassName());
            }

            @Override
            public String toString() {
                return getClass().getSimpleName() + "{" + getFullyQualifiedClassName() + "}";
            }
        }

        private static class ObjectClassDescriptor extends AbstractClassDescriptor {
            ObjectClassDescriptor(String fullName) {
                this(fullName, ensureSimpleName(fullName), createPackage(fullName));
            }

            private ObjectClassDescriptor(String fullName, String simpleName, String packageName) {
                super(fullName, simpleName, packageName);
            }

            @Override
            public JavaClassDescriptor withSimpleClassName(String simpleName) {
                return new ObjectClassDescriptor(getFullyQualifiedClassName(), simpleName, getPackageName());
            }

            @Override
            public JavaClassDescriptor toArrayDescriptor() {
                return From.name(getFullyQualifiedClassName() + "[]");
            }
        }

        private static class PrimitiveClassDescriptor extends AbstractClassDescriptor {
            PrimitiveClassDescriptor(String fullName) {
                super(fullName, fullName, "");
                checkArgument(primitiveClassesByName.containsKey(fullName), "'%s' must be a primitive name", fullName);
            }

            @Override
            Class<?> classForName(ClassLoader classLoader) {
                return primitiveClassesByName.get(getFullyQualifiedClassName());
            }

            @Override
            public boolean isPrimitive() {
                return true;
            }

            @Override
            public JavaClassDescriptor withSimpleClassName(String simpleName) {
                throw new UnsupportedOperationException("It should never make sense to override the simple type of a primitive");
            }

            @Override
            public JavaClassDescriptor toArrayDescriptor() {
                return From.name(getFullyQualifiedClassName() + "[]");
            }
        }

        private static class ArrayClassDescriptor extends AbstractClassDescriptor {
            ArrayClassDescriptor(String fullName) {
                this(fullName, createSimpleName(fullName), createPackageOfComponentType(fullName));
            }

            private ArrayClassDescriptor(String fullName, String simpleName, String packageName) {
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
            public JavaClassDescriptor withSimpleClassName(String simpleName) {
                return new ArrayClassDescriptor(getFullyQualifiedClassName(), simpleName, getPackageName());
            }

            @Override
            public Optional<JavaClassDescriptor> tryGetComponentType() {
                String canonicalName = getCanonicalName(getFullyQualifiedClassName());
                String componentTypeName = canonicalName.substring(0, canonicalName.lastIndexOf("["));
                return Optional.of(JavaClassDescriptor.From.name(componentTypeName));
            }

            @Override
            public JavaClassDescriptor toArrayDescriptor() {
                return new ArrayClassDescriptor("[" + getFullyQualifiedClassName());
            }
        }

        private static String createPackage(String fullName) {
            int packageEnd = fullName.lastIndexOf('.');
            return packageEnd >= 0 ? fullName.substring(0, packageEnd) : "";
        }
    }
}
