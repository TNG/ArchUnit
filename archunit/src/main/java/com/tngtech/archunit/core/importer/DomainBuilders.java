/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodReferenceTarget;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext;
import com.tngtech.archunit.core.domain.Formatters;
import com.tngtech.archunit.core.domain.JavaAccess;
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
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaMethodReference;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.core.domain.SourceCodeLocation;
import com.tngtech.archunit.core.domain.ThrowsClause;
import com.tngtech.archunit.core.domain.TryCatchBlock;
import com.tngtech.archunit.core.domain.properties.HasTypeParameters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeTypeVariable;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createGenericArrayType;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createSource;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createThrowsClause;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createTryCatchBlock;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createTypeVariable;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createWildcardType;
import static com.tngtech.archunit.core.domain.Formatters.ensureCanonicalArrayTypeName;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

@Internal
@SuppressWarnings("UnusedReturnValue")
public final class DomainBuilders {
    private DomainBuilders() {
    }

    static <T extends HasDescription> Map<String, JavaAnnotation<T>> buildAnnotations(T owner, Set<JavaAnnotationBuilder> annotations, ImportedClasses importedClasses) {
        ImmutableMap.Builder<String, JavaAnnotation<T>> result = ImmutableMap.builder();
        for (JavaAnnotationBuilder annotationBuilder : annotations) {
            JavaAnnotation<T> javaAnnotation = annotationBuilder.build(owner, importedClasses);
            result.put(javaAnnotation.getRawType().getName(), javaAnnotation);
        }
        return result.build();
    }

    @Internal
    public static final class JavaEnumConstantBuilder {
        private JavaClass declaringClass;
        private String name;

        JavaEnumConstantBuilder() {
        }

        JavaEnumConstantBuilder withDeclaringClass(JavaClass declaringClass) {
            this.declaringClass = declaringClass;
            return this;
        }

        JavaEnumConstantBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public JavaClass getDeclaringClass() {
            return declaringClass;
        }

        public String getName() {
            return name;
        }

        JavaEnumConstant build() {
            return DomainObjectCreationContext.createJavaEnumConstant(this);
        }
    }

    @Internal
    public abstract static class JavaMemberBuilder<OUTPUT, SELF extends JavaMemberBuilder<OUTPUT, SELF>>
            implements BuilderWithBuildParameter<JavaClass, OUTPUT> {

        private String name;
        private String descriptor;
        private Set<JavaModifier> modifiers;
        private JavaClass owner;
        ImportedClasses importedClasses;
        private int firstLineNumber;

        private JavaMemberBuilder() {
        }

        SELF withName(String name) {
            this.name = name;
            return self();
        }

        SELF withDescriptor(String descriptor) {
            this.descriptor = descriptor;
            return self();
        }

        SELF withModifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
            return self();
        }

        void recordLineNumber(int lineNumber) {
            this.firstLineNumber = this.firstLineNumber == 0 ? lineNumber : Math.min(this.firstLineNumber, lineNumber);
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }

        abstract OUTPUT construct(SELF self, ImportedClasses importedClasses);

        JavaClass get(String typeName) {
            return importedClasses.getOrResolve(typeName);
        }

        public String getName() {
            return name;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public Set<JavaModifier> getModifiers() {
            return modifiers;
        }

        public JavaClass getOwner() {
            return owner;
        }

        public int getFirstLineNumber() {
            return firstLineNumber;
        }

        @Override
        public final OUTPUT build(JavaClass owner, ImportedClasses importedClasses) {
            this.owner = owner;
            this.importedClasses = importedClasses;
            return construct(self(), importedClasses);
        }
    }

    @Internal
    public static final class JavaFieldBuilder extends JavaMemberBuilder<JavaField, JavaFieldBuilder> {
        private Optional<JavaTypeCreationProcess<JavaField>> genericType;
        private JavaClassDescriptor rawType;

        JavaFieldBuilder() {
        }

        JavaFieldBuilder withType(Optional<JavaTypeCreationProcess<JavaField>> genericTypeBuilder, JavaClassDescriptor rawType) {
            this.genericType = checkNotNull(genericTypeBuilder);
            this.rawType = checkNotNull(rawType);
            return self();
        }

        public JavaType getType(JavaField field) {
            return genericType.isPresent()
                    ? genericType.get().finish(field, allTypeParametersInContextOf(field.getOwner()), importedClasses)
                    : importedClasses.getOrResolve(rawType.getFullyQualifiedClassName());
        }

        private static Iterable<JavaTypeVariable<?>> allTypeParametersInContextOf(JavaClass javaClass) {
            return FluentIterable.from(getTypeParametersOf(javaClass)).append(allTypeParametersInEnclosingContextOf(javaClass));
        }

        @Override
        JavaField construct(JavaFieldBuilder builder, ImportedClasses importedClasses) {
            return DomainObjectCreationContext.createJavaField(builder);
        }
    }

    @Internal
    public abstract static class JavaCodeUnitBuilder<OUTPUT, SELF extends JavaCodeUnitBuilder<OUTPUT, SELF>> extends JavaMemberBuilder<OUTPUT, SELF> {
        private Optional<JavaTypeCreationProcess<JavaCodeUnit>> genericReturnType;
        private JavaClassDescriptor rawReturnType;
        private List<JavaTypeCreationProcess<JavaCodeUnit>> genericParameterTypes;
        private List<JavaClassDescriptor> rawParameterTypes;
        private SetMultimap<Integer, JavaAnnotationBuilder> parameterAnnotationsByIndex;
        private JavaCodeUnitTypeParametersBuilder typeParametersBuilder;
        private List<JavaClassDescriptor> throwsDeclarations;

        private JavaCodeUnitBuilder() {
        }

        SELF withReturnType(Optional<JavaTypeCreationProcess<JavaCodeUnit>> genericReturnType, JavaClassDescriptor rawReturnType) {
            this.genericReturnType = genericReturnType;
            this.rawReturnType = rawReturnType;
            return self();
        }

        SELF withParameterTypes(List<JavaTypeCreationProcess<JavaCodeUnit>> genericParameterTypes, List<JavaClassDescriptor> rawParameterTypes) {
            this.genericParameterTypes = genericParameterTypes;
            this.rawParameterTypes = rawParameterTypes;
            return self();
        }

        SELF withParameterAnnotations(SetMultimap<Integer, JavaAnnotationBuilder> parameterAnnotationsByIndex) {
            this.parameterAnnotationsByIndex = parameterAnnotationsByIndex;
            return self();
        }

        SELF withTypeParameters(List<JavaTypeParameterBuilder<JavaCodeUnit>> typeParameterBuilders) {
            this.typeParametersBuilder = new JavaCodeUnitTypeParametersBuilder(typeParameterBuilders);
            return self();
        }

        SELF withThrowsClause(List<JavaClassDescriptor> throwsDeclarations) {
            this.throwsDeclarations = throwsDeclarations;
            return self();
        }

        String getReturnTypeName() {
            return rawReturnType.getFullyQualifiedClassName();
        }

        boolean hasNoParameters() {
            return rawParameterTypes.isEmpty();
        }

        public JavaClass getRawReturnType() {
            return get(rawReturnType.getFullyQualifiedClassName());
        }

        public JavaType getGenericReturnType(JavaCodeUnit codeUnit) {
            return genericReturnType.isPresent()
                    ? genericReturnType.get().finish(codeUnit, allTypeParametersInContextOf(codeUnit), importedClasses)
                    : getRawReturnType();
        }

        private Iterable<JavaTypeVariable<?>> allTypeParametersInContextOf(JavaCodeUnit codeUnit) {
            return FluentIterable.from(getTypeParametersOf(codeUnit)).append(allTypeParametersInEnclosingContextOf(codeUnit));
        }

        public List<JavaClass> getRawParameterTypes() {
            return asJavaClasses(rawParameterTypes);
        }

        public List<JavaType> getGenericParameterTypes(JavaCodeUnit codeUnit) {
            return build(genericParameterTypes, codeUnit);
        }

        private List<JavaType> build(List<JavaTypeCreationProcess<JavaCodeUnit>> genericParameterTypeBuilders, JavaCodeUnit codeUnit) {
            ImmutableList.Builder<JavaType> result = ImmutableList.builder();
            for (JavaTypeCreationProcess<JavaCodeUnit> parameterTypeBuilder : genericParameterTypeBuilders) {
                result.add(parameterTypeBuilder.finish(codeUnit, allTypeParametersInContextOf(codeUnit), importedClasses));
            }
            return result.build();
        }

        public List<JavaTypeVariable<JavaCodeUnit>> getTypeParameters(JavaCodeUnit owner) {
            return typeParametersBuilder.build(owner, importedClasses);
        }

        public <CODE_UNIT extends JavaCodeUnit> ThrowsClause<CODE_UNIT> getThrowsClause(CODE_UNIT codeUnit) {
            return createThrowsClause(codeUnit, asJavaClasses(this.throwsDeclarations));
        }

        private List<JavaClass> asJavaClasses(List<JavaClassDescriptor> descriptors) {
            ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
            for (JavaClassDescriptor javaClassDescriptor : descriptors) {
                result.add(get(javaClassDescriptor.getFullyQualifiedClassName()));
            }
            return result.build();
        }

        public ParameterAnnotationsBuilder getParameterAnnotationsBuilder(int index) {
            return new ParameterAnnotationsBuilder(parameterAnnotationsByIndex.get(index), importedClasses);
        }

        @Internal
        public static class ParameterAnnotationsBuilder {
            private final Iterable<JavaAnnotationBuilder> annotationBuilders;
            private final ImportedClasses importedClasses;

            private ParameterAnnotationsBuilder(Iterable<JavaAnnotationBuilder> annotationBuilders, ImportedClasses importedClasses) {
                this.annotationBuilders = annotationBuilders;
                this.importedClasses = importedClasses;
            }

            public Set<JavaAnnotation<JavaParameter>> build(JavaParameter owner) {
                ImmutableSet.Builder<JavaAnnotation<JavaParameter>> result = ImmutableSet.builder();
                for (DomainBuilders.JavaAnnotationBuilder annotationBuilder : annotationBuilders) {
                    result.add(annotationBuilder.build(owner, importedClasses));
                }
                return result.build();
            }
        }
    }

    @Internal
    public static final class JavaMethodBuilder extends JavaCodeUnitBuilder<JavaMethod, JavaMethodBuilder> {
        private static final Function<JavaMethod, Optional<Object>> NO_ANNOTATION_DEFAULT_VALUE = input -> Optional.empty();
        private Function<JavaMethod, Optional<Object>> createAnnotationDefaultValue = NO_ANNOTATION_DEFAULT_VALUE;

        JavaMethodBuilder() {
        }

        JavaMethodBuilder withAnnotationDefaultValue(Function<JavaMethod, Optional<Object>> createAnnotationDefaultValue) {
            this.createAnnotationDefaultValue = createAnnotationDefaultValue;
            return this;
        }

        @Override
        JavaMethod construct(JavaMethodBuilder builder, ImportedClasses importedClasses) {
            return DomainObjectCreationContext.createJavaMethod(builder, createAnnotationDefaultValue);
        }
    }

    @Internal
    public static final class JavaConstructorBuilder extends JavaCodeUnitBuilder<JavaConstructor, JavaConstructorBuilder> {
        JavaConstructorBuilder() {
        }

        @Override
        JavaConstructor construct(JavaConstructorBuilder builder, ImportedClasses importedClasses) {
            return DomainObjectCreationContext.createJavaConstructor(builder);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    @Internal
    public static final class JavaClassBuilder {
        private final boolean stub;
        private Optional<SourceDescriptor> sourceDescriptor = Optional.empty();
        private Optional<String> sourceFileName = Optional.empty();
        private JavaClassDescriptor descriptor;
        private boolean isInterface;
        private boolean isEnum;
        private boolean isAnnotation;
        private boolean isRecord;
        private boolean isAnonymousClass;
        private boolean isMemberClass;
        private Set<JavaModifier> modifiers = new HashSet<>();

        JavaClassBuilder() {
            this(false);
        }

        private JavaClassBuilder(boolean stub) {
            this.stub = stub;
        }

        JavaClassBuilder withSourceDescriptor(SourceDescriptor sourceDescriptor) {
            this.sourceDescriptor = Optional.of(sourceDescriptor);
            return this;
        }

        JavaClassBuilder withSourceFileName(String sourceFileName) {
            this.sourceFileName = Optional.of(sourceFileName);
            return this;
        }

        JavaClassBuilder withDescriptor(JavaClassDescriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        JavaClassBuilder withInterface(boolean isInterface) {
            this.isInterface = isInterface;
            return this;
        }

        JavaClassBuilder withAnonymousClass(boolean isAnonymousClass) {
            this.isAnonymousClass = isAnonymousClass;
            return this;
        }

        JavaClassBuilder withMemberClass(boolean isMemberClass) {
            this.isMemberClass = isMemberClass;
            return this;
        }

        JavaClassBuilder withEnum(boolean isEnum) {
            this.isEnum = isEnum;
            return this;
        }

        public JavaClassBuilder withAnnotation(boolean isAnnotation) {
            this.isAnnotation = isAnnotation;
            return this;
        }

        JavaClassBuilder withRecord(boolean isRecord) {
            this.isRecord = isRecord;
            return this;
        }

        JavaClassBuilder withModifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        JavaClassBuilder withSimpleName(String simpleName) {
            this.descriptor = descriptor.withSimpleClassName(simpleName);
            return this;
        }

        JavaClass build() {
            return DomainObjectCreationContext.createJavaClass(this);
        }

        public Optional<Source> getSource() {
            return sourceDescriptor.map(value -> createSource(value.getUri(), sourceFileName, value.isMd5InClassSourcesEnabled()));
        }

        public JavaClassDescriptor getDescriptor() {
            return descriptor;
        }

        public boolean isInterface() {
            return isInterface;
        }

        public boolean isEnum() {
            return isEnum;
        }

        public boolean isAnnotation() {
            return isAnnotation;
        }

        public boolean isRecord() {
            return isRecord;
        }

        public boolean isAnonymousClass() {
            return isAnonymousClass;
        }

        public boolean isMemberClass() {
            return isMemberClass;
        }

        public Set<JavaModifier> getModifiers() {
            return modifiers;
        }

        public boolean isStub() {
            return stub;
        }

        static JavaClassBuilder forStub() {
            return new JavaClassBuilder(true);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    @Internal
    public static final class JavaAnnotationBuilder {
        private JavaClassDescriptor type;
        private final Map<String, ValueBuilder> values = new LinkedHashMap<>();
        private ImportedClasses importedClasses;

        JavaAnnotationBuilder() {
        }

        JavaAnnotationBuilder withType(JavaClassDescriptor type) {
            this.type = type;
            return this;
        }

        String getFullyQualifiedClassName() {
            return type.getFullyQualifiedClassName();
        }

        JavaAnnotationBuilder addProperty(String key, ValueBuilder valueBuilder) {
            values.put(key, valueBuilder);
            return this;
        }

        public JavaClass getType() {
            return importedClasses.getOrResolve(type.getFullyQualifiedClassName());
        }

        public <T extends HasDescription> Map<String, Object> getValues(T owner) {
            ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
            for (Map.Entry<String, ValueBuilder> entry : values.entrySet()) {
                entry.getValue().build(owner, importedClasses).ifPresent(value -> result.put(entry.getKey(), value));
            }
            return result.build();
        }

        public <T extends HasDescription> JavaAnnotation<T> build(T owner, ImportedClasses importedClasses) {
            this.importedClasses = importedClasses;
            return DomainObjectCreationContext.createJavaAnnotation(owner, this);
        }

        abstract static class ValueBuilder {
            abstract <T extends HasDescription> Optional<Object> build(T owner, ImportedClasses importedClasses);

            static ValueBuilder fromPrimitiveProperty(Object value) {
                return new ValueBuilder() {
                    @Override
                    <T extends HasDescription> Optional<Object> build(T owner, ImportedClasses unused) {
                        return Optional.of(value);
                    }
                };
            }

            public static ValueBuilder fromEnumProperty(JavaClassDescriptor enumType, String value) {
                return new ValueBuilder() {
                    @Override
                    <T extends HasDescription> Optional<Object> build(T owner, ImportedClasses importedClasses) {
                        return Optional.of(
                                new DomainBuilders.JavaEnumConstantBuilder()
                                        .withDeclaringClass(importedClasses.getOrResolve(enumType.getFullyQualifiedClassName()))
                                        .withName(value)
                                        .build());
                    }
                };
            }

            static ValueBuilder fromClassProperty(JavaClassDescriptor value) {
                return new ValueBuilder() {
                    @Override
                    <T extends HasDescription> Optional<Object> build(T owner, ImportedClasses importedClasses) {
                        return Optional.of(importedClasses.getOrResolve(value.getFullyQualifiedClassName()));
                    }
                };
            }

            static ValueBuilder fromAnnotationProperty(JavaAnnotationBuilder builder) {
                return new ValueBuilder() {
                    @Override
                    <T extends HasDescription> Optional<Object> build(T owner, ImportedClasses importedClasses) {
                        return Optional.of(builder.build(owner, importedClasses));
                    }
                };
            }
        }
    }

    @Internal
    public static final class JavaStaticInitializerBuilder extends JavaCodeUnitBuilder<JavaStaticInitializer, JavaStaticInitializerBuilder> {
        JavaStaticInitializerBuilder() {
            withReturnType(Optional.empty(), JavaClassDescriptor.From.name(void.class.getName()));
            withParameterTypes(emptyList(), emptyList());
            withName(JavaStaticInitializer.STATIC_INITIALIZER_NAME);
            withDescriptor("()V");
            withModifiers(Collections.emptySet());
            withThrowsClause(emptyList());
        }

        @Override
        JavaStaticInitializer construct(JavaStaticInitializerBuilder builder, ImportedClasses importedClasses) {
            return DomainObjectCreationContext.createJavaStaticInitializer(builder);
        }
    }

    interface JavaTypeCreationProcess<OWNER> {
        JavaType finish(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ImportedClasses classes);

        abstract class JavaTypeFinisher {
            private JavaTypeFinisher() {
            }

            abstract JavaType finish(JavaType input, ImportedClasses classes);

            abstract String getFinishedName(String name);

            JavaTypeFinisher after(JavaTypeFinisher other) {
                return new JavaTypeFinisher() {
                    @Override
                    JavaType finish(JavaType input, ImportedClasses classes) {
                        return JavaTypeFinisher.this.finish(other.finish(input, classes), classes);
                    }

                    @Override
                    String getFinishedName(String name) {
                        return JavaTypeFinisher.this.getFinishedName(other.getFinishedName(name));
                    }
                };
            }

            static final JavaTypeFinisher IDENTITY = new JavaTypeFinisher() {
                @Override
                JavaType finish(JavaType input, ImportedClasses classes) {
                    return input;
                }

                @Override
                String getFinishedName(String name) {
                    return name;
                }
            };

            static final JavaTypeFinisher ARRAY_CREATOR = new JavaTypeFinisher() {
                @Override
                public JavaType finish(JavaType componentType, ImportedClasses classes) {
                    JavaClassDescriptor erasureType = JavaClassDescriptor.From.javaClass(componentType.toErasure()).toArrayDescriptor();
                    if (componentType instanceof JavaClass) {
                        return classes.getOrResolve(erasureType.getFullyQualifiedClassName());
                    }

                    JavaClass erasure = classes.getOrResolve(erasureType.getFullyQualifiedClassName());
                    return createGenericArrayType(componentType, erasure);
                }

                @Override
                String getFinishedName(String name) {
                    return name + "[]";
                }
            };
        }
    }

    @Internal
    public static final class JavaTypeParameterBuilder<OWNER extends HasDescription> {
        private final String name;
        private final List<JavaTypeCreationProcess<OWNER>> upperBounds = new ArrayList<>();
        private OWNER owner;
        private ImportedClasses importedClasses;

        JavaTypeParameterBuilder(String name) {
            this.name = checkNotNull(name);
        }

        void addBound(JavaTypeCreationProcess<OWNER> bound) {
            upperBounds.add(bound);
        }

        public JavaTypeVariable<OWNER> build(OWNER owner, ImportedClasses importedClasses) {
            this.owner = owner;
            this.importedClasses = importedClasses;
            return createTypeVariable(name, owner, this.importedClasses.getOrResolve(Object.class.getName()));
        }

        String getName() {
            return name;
        }

        @SuppressWarnings("unchecked") // Iterable is covariant
        public List<JavaType> getUpperBounds(Iterable<? extends JavaTypeVariable<?>> allGenericParametersInContext) {
            return buildJavaTypes(upperBounds, owner, (Iterable<JavaTypeVariable<?>>) allGenericParametersInContext, importedClasses);
        }
    }

    private abstract static class AbstractTypeParametersBuilder<OWNER extends HasDescription> {
        private final List<JavaTypeParameterBuilder<OWNER>> typeParameterBuilders;

        AbstractTypeParametersBuilder(List<JavaTypeParameterBuilder<OWNER>> typeParameterBuilders) {
            this.typeParameterBuilders = typeParameterBuilders;
        }

        final List<JavaTypeVariable<OWNER>> build(OWNER owner, ImportedClasses ImportedClasses) {
            if (typeParameterBuilders.isEmpty()) {
                return emptyList();
            }

            Map<JavaTypeVariable<OWNER>, JavaTypeParameterBuilder<OWNER>> typeArgumentsToBuilders = new LinkedHashMap<>();
            for (JavaTypeParameterBuilder<OWNER> builder : typeParameterBuilders) {
                typeArgumentsToBuilders.put(builder.build(owner, ImportedClasses), builder);
            }
            Set<JavaTypeVariable<?>> allGenericParametersInContext = union(typeParametersFromEnclosingContextOf(owner), typeArgumentsToBuilders.keySet());
            for (Map.Entry<JavaTypeVariable<OWNER>, JavaTypeParameterBuilder<OWNER>> typeParameterToBuilder : typeArgumentsToBuilders.entrySet()) {
                List<JavaType> upperBounds = typeParameterToBuilder.getValue().getUpperBounds(allGenericParametersInContext);
                completeTypeVariable(typeParameterToBuilder.getKey(), upperBounds);
            }
            return ImmutableList.copyOf(typeArgumentsToBuilders.keySet());
        }

        abstract Set<JavaTypeVariable<?>> typeParametersFromEnclosingContextOf(OWNER owner);
    }

    static class JavaClassTypeParametersBuilder extends AbstractTypeParametersBuilder<JavaClass> {
        JavaClassTypeParametersBuilder(List<JavaTypeParameterBuilder<JavaClass>> typeParameterBuilders) {
            super(typeParameterBuilders);
        }

        @Override
        Set<JavaTypeVariable<?>> typeParametersFromEnclosingContextOf(JavaClass javaClass) {
            return allTypeParametersInEnclosingContextOf(javaClass);
        }
    }

    static class JavaCodeUnitTypeParametersBuilder extends AbstractTypeParametersBuilder<JavaCodeUnit> {
        JavaCodeUnitTypeParametersBuilder(List<JavaTypeParameterBuilder<JavaCodeUnit>> typeParameterBuilders) {
            super(typeParameterBuilders);
        }

        @Override
        Set<JavaTypeVariable<?>> typeParametersFromEnclosingContextOf(JavaCodeUnit codeUnit) {
            return allTypeParametersInEnclosingContextOf(codeUnit);
        }
    }

    private static Set<JavaTypeVariable<?>> allTypeParametersInEnclosingContextOf(JavaCodeUnit codeUnit) {
        JavaClass declaringClass = codeUnit.getOwner();
        return FluentIterable.from(getTypeParametersOf(declaringClass))
                .append(allTypeParametersInEnclosingContextOf(declaringClass))
                .toSet();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<JavaTypeVariable<?>> getTypeParametersOf(HasTypeParameters<?> hasTypeParameters) {
        List<? extends JavaTypeVariable<?>> result = hasTypeParameters.getTypeParameters();
        return (List) result;
    }

    private static Set<JavaTypeVariable<?>> allTypeParametersInEnclosingContextOf(JavaClass javaClass) {
        Set<JavaTypeVariable<?>> result = new HashSet<>();
        while (javaClass.getEnclosingClass().isPresent()) {
            if (javaClass.getEnclosingCodeUnit().isPresent()) {
                // Note that there can't be a case where we could have an enclosing code unit without an enclosing class,
                // since by definition the class where the enclosing code unit is declared in is an enclosing class
                result.addAll(javaClass.getEnclosingCodeUnit().get().getTypeParameters());
            }
            result.addAll(javaClass.getEnclosingClass().get().getTypeParameters());
            javaClass = javaClass.getEnclosingClass().get();
        }
        return result;
    }

    interface JavaTypeBuilder<OWNER extends HasDescription> {
        JavaType build(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ImportedClasses importedClasses);
    }

    @Internal
    public static final class JavaWildcardTypeBuilder<OWNER extends HasDescription> implements JavaTypeBuilder<OWNER> {
        private final List<JavaTypeCreationProcess<OWNER>> lowerBoundCreationProcesses = new ArrayList<>();
        private final List<JavaTypeCreationProcess<OWNER>> upperBoundCreationProcesses = new ArrayList<>();
        private OWNER owner;
        private Iterable<JavaTypeVariable<?>> allTypeParametersInContext;
        private ImportedClasses importedClasses;

        JavaWildcardTypeBuilder() {
        }

        public JavaWildcardTypeBuilder<OWNER> addLowerBound(JavaTypeCreationProcess<OWNER> boundCreationProcess) {
            lowerBoundCreationProcesses.add(boundCreationProcess);
            return this;
        }

        public JavaWildcardTypeBuilder<OWNER> addUpperBound(JavaTypeCreationProcess<OWNER> boundCreationProcess) {
            upperBoundCreationProcesses.add(boundCreationProcess);
            return this;
        }

        @Override
        public JavaWildcardType build(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ImportedClasses importedClasses) {
            this.owner = owner;
            this.allTypeParametersInContext = allTypeParametersInContext;
            this.importedClasses = importedClasses;
            return createWildcardType(this);
        }

        public List<JavaType> getUpperBounds() {
            return buildJavaTypes(upperBoundCreationProcesses, owner, allTypeParametersInContext, importedClasses);
        }

        public List<JavaType> getLowerBounds() {
            return buildJavaTypes(lowerBoundCreationProcesses, owner, allTypeParametersInContext, importedClasses);
        }

        public JavaClass getUnboundErasureType(List<JavaType> upperBounds) {
            return DomainBuilders.getUnboundErasureType(upperBounds, importedClasses);
        }
    }

    static class JavaParameterizedTypeBuilder<OWNER extends HasDescription> implements JavaTypeBuilder<OWNER> {
        private final JavaClassDescriptor type;
        private final List<JavaTypeCreationProcess<OWNER>> typeArgumentCreationProcesses = new ArrayList<>();

        JavaParameterizedTypeBuilder(JavaClassDescriptor type) {
            this.type = type;
        }

        void addTypeArgument(JavaTypeCreationProcess<OWNER> typeCreationProcess) {
            typeArgumentCreationProcesses.add(typeCreationProcess);
        }

        @Override
        public JavaType build(OWNER owner, Iterable<JavaTypeVariable<?>> allTypeParametersInContext, ImportedClasses classes) {
            List<JavaType> typeArguments = buildJavaTypes(typeArgumentCreationProcesses, owner, allTypeParametersInContext, classes);
            return typeArguments.isEmpty()
                    ? classes.getOrResolve(type.getFullyQualifiedClassName())
                    : new ImportedParameterizedType(classes.getOrResolve(type.getFullyQualifiedClassName()), typeArguments);
        }

        String getTypeName() {
            return type.getFullyQualifiedClassName();
        }

        JavaParameterizedTypeBuilder<OWNER> forInnerClass(String simpleInnerClassName) {
            return new JavaParameterizedTypeBuilder<>(JavaClassDescriptorImporter.createFromAsmObjectTypeName(
                    type.getFullyQualifiedClassName() + '$' + simpleInnerClassName));
        }
    }

    private static <OWNER> List<JavaType> buildJavaTypes(List<? extends JavaTypeCreationProcess<OWNER>> typeCreationProcesses, OWNER owner, Iterable<JavaTypeVariable<?>> allGenericParametersInContext, ImportedClasses classes) {
        ImmutableList.Builder<JavaType> result = ImmutableList.builder();
        for (JavaTypeCreationProcess<OWNER> typeCreationProcess : typeCreationProcesses) {
            result.add(typeCreationProcess.finish(owner, allGenericParametersInContext, classes));
        }
        return result.build();
    }

    private static JavaClass getUnboundErasureType(List<JavaType> upperBounds, ImportedClasses importedClasses) {
        return upperBounds.size() > 0
                ? upperBounds.get(0).toErasure()
                : importedClasses.getOrResolve(Object.class.getName());
    }

    @Internal
    interface BuilderWithBuildParameter<PARAMETER, VALUE> {
        VALUE build(PARAMETER parameter, ImportedClasses importedClasses);

        @Internal
        class BuildFinisher {
            static <PARAMETER, VALUE> Set<VALUE> build(
                    Set<? extends BuilderWithBuildParameter<PARAMETER, ? extends VALUE>> builders,
                    PARAMETER parameter,
                    ImportedClasses importedClasses) {
                return build(builders.stream(), parameter, importedClasses);
            }

            static <PARAMETER, VALUE> Set<VALUE> build(
                    Stream<? extends BuilderWithBuildParameter<PARAMETER, ? extends VALUE>> builders,
                    PARAMETER parameter,
                    ImportedClasses importedClasses) {
                checkNotNull(builders);
                checkNotNull(parameter);

                return builders.map(builder -> builder.build(parameter, importedClasses)).collect(toImmutableSet());
            }
        }
    }

    @Internal
    public static class TryCatchBlockBuilder {
        private Set<JavaClass> caughtThrowables;
        private int lineNumber;
        private JavaCodeUnit owner;
        private final Set<JavaAccess<?>> accessesContainedInTryBlock = new HashSet<>();
        private Set<RawAccessRecord> rawAccessesContainedInTryBlock;
        private boolean declaredInLambda;

        TryCatchBlockBuilder() {
        }

        TryCatchBlockBuilder withCaughtThrowables(Set<JavaClass> caughtThrowables) {
            this.caughtThrowables = caughtThrowables;
            return this;
        }

        TryCatchBlockBuilder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        TryCatchBlockBuilder withRawAccessesContainedInTryBlock(Set<RawAccessRecord> accessRecords) {
            this.rawAccessesContainedInTryBlock = accessRecords;
            return this;
        }

        void addIfContainedInTryBlock(RawAccessRecord rawRecord, JavaAccess<?> access) {
            if (rawAccessesContainedInTryBlock.contains(rawRecord)) {
                accessesContainedInTryBlock.add(access);
            }
        }

        TryCatchBlockBuilder withDeclaredInLambda(boolean declaredInLambda) {
            this.declaredInLambda = declaredInLambda;
            return this;
        }

        public TryCatchBlock build(JavaCodeUnit owner) {
            this.owner = owner;
            return createTryCatchBlock(this);
        }

        public JavaCodeUnit getOwner() {
            return owner;
        }

        public Set<JavaAccess<?>> getAccessesContainedInTryBlock() {
            return accessesContainedInTryBlock;
        }

        public Set<JavaClass> getCaughtThrowables() {
            return caughtThrowables;
        }

        public SourceCodeLocation getSourceCodeLocation() {
            return SourceCodeLocation.of(owner.getOwner(), lineNumber);
        }

        public boolean isDeclaredInLambda() {
            return declaredInLambda;
        }
    }

    @Internal
    public abstract static class JavaAccessBuilder<TARGET extends AccessTarget, SELF extends JavaAccessBuilder<TARGET, SELF>> {
        private JavaCodeUnit origin;
        private TARGET target;
        private int lineNumber;
        private boolean declaredInLambda;

        private JavaAccessBuilder() {
        }

        SELF withOrigin(JavaCodeUnit origin) {
            this.origin = origin;
            return self();
        }

        SELF withTarget(TARGET target) {
            this.target = target;
            return self();
        }

        SELF withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return self();
        }

        SELF withDeclaredInLambda(boolean declaredInLambda) {
            this.declaredInLambda = declaredInLambda;
            return self();
        }

        public JavaCodeUnit getOrigin() {
            return origin;
        }

        public TARGET getTarget() {
            return target;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        @SuppressWarnings("unchecked")
        private SELF self() {
            return (SELF) this;
        }

        public boolean isDeclaredInLambda() {
            return declaredInLambda;
        }
    }

    @Internal
    public static class JavaFieldAccessBuilder extends JavaAccessBuilder<FieldAccessTarget, JavaFieldAccessBuilder> {
        private AccessType accessType;

        JavaFieldAccessBuilder() {
        }

        JavaFieldAccessBuilder withAccessType(AccessType accessType) {
            this.accessType = accessType;
            return this;
        }

        public AccessType getAccessType() {
            return accessType;
        }

        JavaFieldAccess build() {
            return DomainObjectCreationContext.createJavaFieldAccess(this);
        }
    }

    @Internal
    public static final class JavaMethodCallBuilder extends JavaAccessBuilder<MethodCallTarget, JavaMethodCallBuilder> {
        JavaMethodCallBuilder() {
        }

        JavaMethodCall build() {
            return DomainObjectCreationContext.createJavaMethodCall(this);
        }
    }

    @Internal
    public static final class JavaMethodReferenceBuilder extends JavaAccessBuilder<MethodReferenceTarget, JavaMethodReferenceBuilder> {
        JavaMethodReferenceBuilder() {
        }

        JavaMethodReference build() {
            return DomainObjectCreationContext.createJavaMethodReference(this);
        }
    }

    @Internal
    public static class JavaConstructorCallBuilder extends JavaAccessBuilder<ConstructorCallTarget, JavaConstructorCallBuilder> {
        JavaConstructorCallBuilder() {
        }

        JavaConstructorCall build() {
            return DomainObjectCreationContext.createJavaConstructorCall(this);
        }
    }

    @Internal
    public static class JavaConstructorReferenceBuilder extends JavaAccessBuilder<ConstructorReferenceTarget, JavaConstructorReferenceBuilder> {
        JavaConstructorReferenceBuilder() {
        }

        JavaConstructorReference build() {
            return DomainObjectCreationContext.createJavaConstructorReference(this);
        }
    }

    @Internal
    public abstract static class AccessTargetBuilder<MEMBER extends JavaMember, TARGET extends AccessTarget, SELF extends AccessTargetBuilder<MEMBER, TARGET, SELF>> {
        private final Function<SELF, TARGET> createTarget;

        private JavaClass owner;
        private String name;
        Supplier<Optional<MEMBER>> member;

        AccessTargetBuilder(Function<SELF, TARGET> createTarget) {
            this.createTarget = createTarget;
        }

        SELF withOwner(JavaClass owner) {
            this.owner = owner;
            return self();
        }

        SELF withName(String name) {
            this.name = name;
            return self();
        }

        SELF withMember(Supplier<Optional<MEMBER>> member) {
            this.member = member;
            return self();
        }

        TARGET build() {
            return createTarget.apply(self());
        }

        public JavaClass getOwner() {
            return owner;
        }

        public String getName() {
            return name;
        }

        public Supplier<Optional<MEMBER>> getMember() {
            return member;
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }

        public abstract String getFullName();
    }

    @Internal
    public static final class FieldAccessTargetBuilder extends AccessTargetBuilder<JavaField, FieldAccessTarget, FieldAccessTargetBuilder> {
        private JavaClass type;

        FieldAccessTargetBuilder() {
            super(DomainObjectCreationContext::createFieldAccessTarget);
        }

        FieldAccessTargetBuilder withType(JavaClass type) {
            this.type = type;
            return this;
        }

        public JavaClass getType() {
            return type;
        }

        @Override
        public String getFullName() {
            return getOwner().getName() + "." + getName();
        }
    }

    @Internal
    public static class CodeUnitAccessTargetBuilder<CODE_UNIT extends JavaCodeUnit, ACCESS_TARGET extends CodeUnitAccessTarget>
            extends AccessTargetBuilder<CODE_UNIT, ACCESS_TARGET, CodeUnitAccessTargetBuilder<CODE_UNIT, ACCESS_TARGET>> {
        private List<JavaClass> parameters;
        private JavaClass returnType;

        private CodeUnitAccessTargetBuilder(Function<CodeUnitAccessTargetBuilder<CODE_UNIT, ACCESS_TARGET>, ACCESS_TARGET> createTarget) {
            super(createTarget);
        }

        CodeUnitAccessTargetBuilder<CODE_UNIT, ACCESS_TARGET> withParameters(List<JavaClass> parameters) {
            this.parameters = parameters;
            return self();
        }

        CodeUnitAccessTargetBuilder<CODE_UNIT, ACCESS_TARGET> withReturnType(JavaClass returnType) {
            this.returnType = returnType;
            return self();
        }

        public List<JavaClass> getParameters() {
            return parameters;
        }

        public JavaClass getReturnType() {
            return returnType;
        }

        public String getFullName() {
            return Formatters.formatMethod(getOwner().getName(), getName(), namesOf(parameters));
        }
    }

    public static CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorCallTarget> newConstructorCallTargetBuilder() {
        return new CodeUnitAccessTargetBuilder<>(DomainObjectCreationContext::createConstructorCallTarget).withName(CONSTRUCTOR_NAME);
    }

    public static CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorReferenceTarget> newConstructorReferenceTargetBuilder() {
        return new CodeUnitAccessTargetBuilder<>(DomainObjectCreationContext::createConstructorReferenceTarget).withName(CONSTRUCTOR_NAME);
    }

    public static CodeUnitAccessTargetBuilder<JavaMethod, MethodCallTarget> newMethodCallTargetBuilder() {
        return new CodeUnitAccessTargetBuilder<>(DomainObjectCreationContext::createMethodCallTarget);
    }

    public static CodeUnitAccessTargetBuilder<JavaMethod, MethodReferenceTarget> newMethodReferenceTargetBuilder() {
        return new CodeUnitAccessTargetBuilder<>(DomainObjectCreationContext::createMethodReferenceTarget);
    }

    private static class ImportedParameterizedType implements JavaParameterizedType {
        private final JavaType type;
        private final List<JavaType> typeArguments;

        ImportedParameterizedType(JavaType type, List<JavaType> typeArguments) {
            checkArgument(typeArguments.size() > 0,
                    "Parameterized type cannot be created without type arguments. This is likely a bug.");

            this.type = type;
            this.typeArguments = typeArguments;
        }

        @Override
        public String getName() {
            return type.getName() + formatTypeArguments();
        }

        @Override
        public JavaClass toErasure() {
            return type.toErasure();
        }

        @Override
        public List<JavaType> getActualTypeArguments() {
            return typeArguments;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + getName() + '}';
        }

        private String formatTypeArguments() {
            return typeArguments.stream()
                    .map(typeArgument -> ensureCanonicalArrayTypeName(typeArgument.getName()))
                    .collect(joining(", ", "<", ">"));
        }
    }
}
