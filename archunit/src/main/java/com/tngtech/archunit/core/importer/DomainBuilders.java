/*
 * Copyright 2019 TNG Technology Consulting GmbH
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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext;
import com.tngtech.archunit.core.domain.Formatters;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassList;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.core.domain.ThrowsClause;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createJavaClassList;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createSource;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createThrowsClause;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;

@Internal
public final class DomainBuilders {
    private DomainBuilders() {
    }

    static Map<String, JavaAnnotation> buildAnnotations(Set<JavaAnnotationBuilder> annotations, ClassesByTypeName importedClasses) {
        ImmutableMap.Builder<String, JavaAnnotation> result = ImmutableMap.builder();
        for (JavaAnnotationBuilder annotationBuilder : annotations) {
            JavaAnnotation javaAnnotation = annotationBuilder.build(importedClasses);
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

        JavaEnumConstantBuilder withDeclaringClass(final JavaClass declaringClass) {
            this.declaringClass = declaringClass;
            return this;
        }

        JavaEnumConstantBuilder withName(final String name) {
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
        private Set<JavaAnnotationBuilder> annotations;
        private Set<JavaModifier> modifiers;
        private JavaClass owner;
        private ClassesByTypeName importedClasses;

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

        SELF withAnnotations(Set<JavaAnnotationBuilder> annotations) {
            this.annotations = annotations;
            return self();
        }

        SELF withModifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
            return self();
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }

        abstract OUTPUT construct(SELF self, ClassesByTypeName importedClasses);

        JavaClass get(String typeName) {
            return importedClasses.get(typeName);
        }

        public String getName() {
            return name;
        }

        public Supplier<Map<String, JavaAnnotation>> getAnnotations() {
            return Suppliers.memoize(new Supplier<Map<String, JavaAnnotation>>() {
                @Override
                public Map<String, JavaAnnotation> get() {
                    return buildAnnotations(annotations, importedClasses);
                }
            });
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

        @Override
        public final OUTPUT build(JavaClass owner, ClassesByTypeName importedClasses) {
            this.owner = owner;
            this.importedClasses = importedClasses;
            return construct(self(), importedClasses);
        }
    }

    @Internal
    public static final class JavaFieldBuilder extends JavaMemberBuilder<JavaField, JavaFieldBuilder> {
        private JavaType type;

        JavaFieldBuilder() {
        }

        JavaFieldBuilder withType(JavaType type) {
            this.type = type;
            return self();
        }

        public JavaClass getType() {
            return get(type.getName());
        }

        @Override
        JavaField construct(JavaFieldBuilder builder, ClassesByTypeName importedClasses) {
            return DomainObjectCreationContext.createJavaField(builder);
        }
    }

    @Internal
    public abstract static class JavaCodeUnitBuilder<OUTPUT, SELF extends JavaCodeUnitBuilder<OUTPUT, SELF>> extends JavaMemberBuilder<OUTPUT, SELF> {
        private JavaType returnType;
        private List<JavaType> parameters;
        private List<JavaType> throwsDeclarations;

        private JavaCodeUnitBuilder() {
        }

        SELF withReturnType(JavaType type) {
            returnType = type;
            return self();
        }

        SELF withParameters(List<JavaType> parameters) {
            this.parameters = parameters;
            return self();
        }

        SELF withThrowsClause(List<JavaType> throwsDeclarations) {
            this.throwsDeclarations = throwsDeclarations;
            return self();
        }

        public JavaClass getReturnType() {
            return get(returnType.getName());
        }

        public JavaClassList getParameters() {
            return createJavaClassList(asJavaClasses(parameters));
        }

        public <CODE_UNIT extends JavaCodeUnit> ThrowsClause<CODE_UNIT> getThrowsClause(CODE_UNIT codeUnit) {
            return createThrowsClause(codeUnit, asJavaClasses(this.throwsDeclarations));
        }

        private List<JavaClass> asJavaClasses(List<JavaType> javaTypes) {
            ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
            for (JavaType javaType : javaTypes) {
                result.add(get(javaType.getName()));
            }
            return result.build();
        }
    }

    @Internal
    public static final class JavaMethodBuilder extends JavaCodeUnitBuilder<JavaMethod, JavaMethodBuilder> {
        private Optional<JavaAnnotationBuilder.ValueBuilder> annotationDefaultValueBuilder = Optional.absent();
        private Supplier<Optional<Object>> annotationDefaultValue = Suppliers.ofInstance(Optional.absent());

        JavaMethodBuilder() {
        }

        JavaMethodBuilder withAnnotationDefaultValue(JavaAnnotationBuilder.ValueBuilder defaultValue) {
            annotationDefaultValueBuilder = Optional.of(defaultValue);
            return this;
        }

        public Supplier<Optional<Object>> getAnnotationDefaultValue() {
            return annotationDefaultValue;
        }

        @Override
        JavaMethod construct(JavaMethodBuilder builder, final ClassesByTypeName importedClasses) {
            if (annotationDefaultValueBuilder.isPresent()) {
                annotationDefaultValue = Suppliers.memoize(new Supplier<Optional<Object>>() {
                    @Override
                    public Optional<Object> get() {
                        return annotationDefaultValueBuilder.get().build(importedClasses);
                    }
                });
            }
            return DomainObjectCreationContext.createJavaMethod(builder);
        }
    }

    @Internal
    public static final class JavaConstructorBuilder extends JavaCodeUnitBuilder<JavaConstructor, JavaConstructorBuilder> {
        JavaConstructorBuilder() {
        }

        @Override
        JavaConstructor construct(JavaConstructorBuilder builder, ClassesByTypeName importedClasses) {
            return DomainObjectCreationContext.createJavaConstructor(builder);
        }
    }

    @Internal
    public static final class JavaClassBuilder {
        private Optional<URI> sourceURI = Optional.absent();
        private Optional<String> sourceFileName = Optional.absent();
        private JavaType javaType;
        private boolean isInterface;
        private boolean isEnum;
        private Set<JavaModifier> modifiers = new HashSet<>();

        JavaClassBuilder() {
        }

        JavaClassBuilder withSourceUri(URI sourceUri) {
            this.sourceURI = Optional.of(sourceUri);
            return this;
        }

        JavaClassBuilder withSourceFileName(String sourceFileName) {
            this.sourceFileName = Optional.of(sourceFileName);
            return this;
        }

        JavaClassBuilder withType(JavaType javaType) {
            this.javaType = javaType;
            return this;
        }

        JavaClassBuilder withInterface(boolean isInterface) {
            this.isInterface = isInterface;
            return this;
        }

        JavaClassBuilder withEnum(boolean isEnum) {
            this.isEnum = isEnum;
            return this;
        }

        JavaClassBuilder withModifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        JavaClass build() {
            return DomainObjectCreationContext.createJavaClass(this);
        }

        public Optional<Source> getSource() {
            return sourceURI.isPresent() ? Optional.of(createSource(sourceURI.get(), sourceFileName)) : Optional.<Source>absent();
        }

        public JavaType getJavaType() {
            return javaType;
        }

        public boolean isInterface() {
            return isInterface;
        }

        public boolean isEnum() {
            return isEnum;
        }

        public Set<JavaModifier> getModifiers() {
            return modifiers;
        }
    }

    @Internal
    public static final class JavaAnnotationBuilder {
        private JavaType type;
        private final Map<String, ValueBuilder> values = new HashMap<>();
        private ClassesByTypeName importedClasses;

        JavaAnnotationBuilder() {
        }

        JavaAnnotationBuilder withType(JavaType type) {
            this.type = type;
            return this;
        }

        JavaType getJavaType() {
            return type;
        }

        JavaAnnotationBuilder addProperty(String key, ValueBuilder valueBuilder) {
            values.put(key, valueBuilder);
            return this;
        }

        JavaAnnotation build(ClassesByTypeName importedClasses) {
            this.importedClasses = importedClasses;
            return DomainObjectCreationContext.createJavaAnnotation(this);
        }

        public JavaClass getType() {
            return importedClasses.get(type.getName());
        }

        public Map<String, Object> getValues() {
            ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
            for (Map.Entry<String, ValueBuilder> entry : values.entrySet()) {
                Optional<Object> value = entry.getValue().build(importedClasses);
                if (value.isPresent()) {
                    result.put(entry.getKey(), value.get());
                }
            }
            addDefaultValues(result, importedClasses);
            return result.build();
        }

        private void addDefaultValues(ImmutableMap.Builder<String, Object> result, ClassesByTypeName importedClasses) {
            for (JavaMethod method : importedClasses.get(type.getName()).getMethods()) {
                if (!values.containsKey(method.getName()) && method.getDefaultValue().isPresent()) {
                    result.put(method.getName(), method.getDefaultValue().get());
                }
            }
        }

        abstract static class ValueBuilder {
            abstract Optional<Object> build(ClassesByTypeName importedClasses);

            static ValueBuilder ofFinished(final Object value) {
                return new ValueBuilder() {
                    @Override
                    Optional<Object> build(ClassesByTypeName importedClasses) {
                        return Optional.of(value);
                    }
                };
            }

            static ValueBuilder from(final JavaAnnotationBuilder builder) {
                return new ValueBuilder() {
                    @Override
                    Optional<Object> build(ClassesByTypeName importedClasses) {
                        return Optional.<Object>of(builder.build(importedClasses));
                    }
                };
            }
        }
    }

    @Internal
    public static final class JavaStaticInitializerBuilder extends JavaCodeUnitBuilder<JavaStaticInitializer, JavaStaticInitializerBuilder> {
        JavaStaticInitializerBuilder() {
            withReturnType(JavaType.From.name(void.class.getName()));
            withParameters(Collections.<JavaType>emptyList());
            withName(JavaStaticInitializer.STATIC_INITIALIZER_NAME);
            withDescriptor("()V");
            withAnnotations(Collections.<JavaAnnotationBuilder>emptySet());
            withModifiers(Collections.<JavaModifier>emptySet());
            withThrowsClause(Collections.<JavaType>emptyList());
        }

        @Override
        JavaStaticInitializer construct(JavaStaticInitializerBuilder builder, ClassesByTypeName importedClasses) {
            return DomainObjectCreationContext.createJavaStaticInitializer(builder);
        }
    }

    @Internal
    interface BuilderWithBuildParameter<PARAMETER, VALUE> {
        VALUE build(PARAMETER parameter, ClassesByTypeName importedClasses);

        @Internal
        class BuildFinisher {
            static <PARAMETER, VALUE> Set<VALUE> build(
                    Set<? extends BuilderWithBuildParameter<PARAMETER, ? extends VALUE>> builders,
                    PARAMETER parameter,
                    ClassesByTypeName importedClasses) {
                checkNotNull(builders);
                checkNotNull(parameter);

                ImmutableSet.Builder<VALUE> result = ImmutableSet.builder();
                for (BuilderWithBuildParameter<PARAMETER, ? extends VALUE> builder : builders) {
                    result.add(builder.build(parameter, importedClasses));
                }
                return result.build();
            }
        }
    }

    @Internal
    public abstract static class JavaAccessBuilder<TARGET extends AccessTarget, SELF extends JavaAccessBuilder<TARGET, SELF>> {
        private JavaCodeUnit origin;
        private TARGET target;
        private int lineNumber;

        private JavaAccessBuilder() {
        }

        SELF withOrigin(final JavaCodeUnit origin) {
            this.origin = origin;
            return self();
        }

        SELF withTarget(final TARGET target) {
            this.target = target;
            return self();
        }

        SELF withLineNumber(final int lineNumber) {
            this.lineNumber = lineNumber;
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
    }

    @Internal
    public static class JavaFieldAccessBuilder extends JavaAccessBuilder<FieldAccessTarget, JavaFieldAccessBuilder> {
        private AccessType accessType;

        JavaFieldAccessBuilder() {
        }

        JavaFieldAccessBuilder withAccessType(final AccessType accessType) {
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
    public static class JavaConstructorCallBuilder extends JavaAccessBuilder<ConstructorCallTarget, JavaConstructorCallBuilder> {
        JavaConstructorCallBuilder() {
        }

        JavaConstructorCall build() {
            return DomainObjectCreationContext.createJavaConstructorCall(this);
        }
    }

    abstract static class AccessTargetBuilder<SELF extends AccessTargetBuilder<SELF>> {
        private JavaClass owner;
        private String name;

        private AccessTargetBuilder() {
        }

        SELF withOwner(final JavaClass owner) {
            this.owner = owner;
            return self();
        }

        SELF withName(final String name) {
            this.name = name;
            return self();
        }

        public JavaClass getOwner() {
            return owner;
        }

        public String getName() {
            return name;
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }
    }

    @Internal
    public static final class FieldAccessTargetBuilder extends AccessTargetBuilder<FieldAccessTargetBuilder> {
        private JavaClass type;
        private Supplier<Optional<JavaField>> field;

        FieldAccessTargetBuilder() {
        }

        FieldAccessTargetBuilder withType(final JavaClass type) {
            this.type = type;
            return this;
        }

        FieldAccessTargetBuilder withField(final Supplier<Optional<JavaField>> field) {
            this.field = field;
            return this;
        }

        public JavaClass getType() {
            return type;
        }

        public Supplier<Optional<JavaField>> getField() {
            return field;
        }

        public String getFullName() {
            return getOwner().getName() + "." + getName();
        }

        FieldAccessTarget build() {
            return DomainObjectCreationContext.createFieldAccessTarget(this);
        }
    }

    @Internal
    public abstract static class CodeUnitCallTargetBuilder<SELF extends CodeUnitCallTargetBuilder<SELF>>
            extends AccessTargetBuilder<SELF> {
        private JavaClassList parameters;
        private JavaClass returnType;

        private CodeUnitCallTargetBuilder() {
        }

        SELF withParameters(final JavaClassList parameters) {
            this.parameters = parameters;
            return self();
        }

        SELF withReturnType(final JavaClass returnType) {
            this.returnType = returnType;
            return self();
        }

        public JavaClassList getParameters() {
            return parameters;
        }

        public JavaClass getReturnType() {
            return returnType;
        }

        public String getFullName() {
            return Formatters.formatMethod(getOwner().getName(), getName(), parameters);
        }
    }

    @Internal
    public static final class ConstructorCallTargetBuilder extends CodeUnitCallTargetBuilder<ConstructorCallTargetBuilder> {
        private Supplier<Optional<JavaConstructor>> constructor;

        ConstructorCallTargetBuilder() {
            withName(CONSTRUCTOR_NAME);
        }

        ConstructorCallTargetBuilder withConstructor(Supplier<Optional<JavaConstructor>> constructor) {
            this.constructor = constructor;
            return self();
        }

        public Supplier<Optional<JavaConstructor>> getConstructor() {
            return constructor;
        }

        ConstructorCallTarget build() {
            return DomainObjectCreationContext.createConstructorCallTarget(this);
        }
    }

    @Internal
    public static final class MethodCallTargetBuilder extends CodeUnitCallTargetBuilder<MethodCallTargetBuilder> {
        private Supplier<Set<JavaMethod>> methods;

        MethodCallTargetBuilder() {
        }

        MethodCallTargetBuilder withMethods(final Supplier<Set<JavaMethod>> methods) {
            this.methods = methods;
            return this;
        }

        public Supplier<Set<JavaMethod>> getMethods() {
            return methods;
        }

        MethodCallTarget build() {
            return DomainObjectCreationContext.createMethodCallTarget(this);
        }
    }
}
