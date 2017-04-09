package com.tngtech.archunit.core.importer;

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
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.ClassesByTypeName;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaConstructor;
import com.tngtech.archunit.core.JavaEnumConstant;
import com.tngtech.archunit.core.JavaField;
import com.tngtech.archunit.core.JavaMethod;
import com.tngtech.archunit.core.JavaModifier;
import com.tngtech.archunit.core.JavaStaticInitializer;
import com.tngtech.archunit.core.JavaType;
import com.tngtech.archunit.core.Source;

import static com.google.common.base.Preconditions.checkNotNull;

public class DomainBuilders {
    public static Map<String, JavaAnnotation> buildAnnotations(Set<JavaAnnotationBuilder> annotations, ClassesByTypeName importedClasses) {
        ImmutableMap.Builder<String, JavaAnnotation> result = ImmutableMap.builder();
        for (JavaAnnotationBuilder annotationBuilder : annotations) {
            JavaAnnotation javaAnnotation = annotationBuilder.build(importedClasses);
            result.put(javaAnnotation.getType().getName(), javaAnnotation);
        }
        return result.build();
    }

    public static final class JavaConstructorBuilder extends JavaCodeUnitBuilder<JavaConstructor, JavaConstructorBuilder> {
        @Override
        JavaConstructor construct(JavaConstructorBuilder builder, ClassesByTypeName importedClasses) {
            return new JavaConstructor(builder);
        }
    }

    public static class JavaEnumConstantBuilder {
        private JavaClass declaringClass;
        private String name;

        public JavaEnumConstantBuilder withDeclaringClass(final JavaClass declaringClass) {
            this.declaringClass = declaringClass;
            return this;
        }

        public JavaEnumConstantBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public JavaClass getDeclaringClass() {
            return declaringClass;
        }

        public String getName() {
            return name;
        }

        public JavaEnumConstant build() {
            return new JavaEnumConstant(this);
        }
    }

    public static class JavaMethodBuilder extends JavaCodeUnitBuilder<JavaMethod, JavaMethodBuilder> {
        private Optional<JavaAnnotationBuilder.ValueBuilder> annotationDefaultValueBuilder = Optional.absent();
        private Supplier<Optional<Object>> annotationDefaultValue = Suppliers.ofInstance(Optional.absent());

        public JavaMethodBuilder withAnnotationDefaultValue(JavaAnnotationBuilder.ValueBuilder defaultValue) {
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
            return new JavaMethod(builder);
        }
    }

    public abstract static class JavaMemberBuilder<OUTPUT, SELF extends JavaMemberBuilder<OUTPUT, SELF>> implements BuilderWithBuildParameter<JavaClass, OUTPUT> {
        private String name;
        private String descriptor;
        private Set<JavaAnnotationBuilder> annotations;
        private Set<JavaModifier> modifiers;
        private JavaClass owner;
        private ClassesByTypeName importedClasses;

        public SELF withName(String name) {
            this.name = name;
            return self();
        }

        public String getName() {
            return name;
        }

        public SELF withDescriptor(String descriptor) {
            this.descriptor = descriptor;
            return self();
        }

        public SELF withAnnotations(Set<JavaAnnotationBuilder> annotations) {
            this.annotations = annotations;
            return self();
        }

        public SELF withModifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
            return self();
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }

        JavaClass get(String typeName) {
            return importedClasses.get(typeName);
        }

        abstract OUTPUT construct(SELF self, ClassesByTypeName importedClasses);

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

    public abstract static class JavaCodeUnitBuilder<OUTPUT, SELF extends JavaCodeUnitBuilder<OUTPUT, SELF>> extends JavaMemberBuilder<OUTPUT, SELF> {
        private JavaType returnType;
        private List<JavaType> parameters;

        public SELF withReturnType(JavaType type) {
            returnType = type;
            return self();
        }

        public SELF withParameters(List<JavaType> parameters) {
            this.parameters = parameters;
            return self();
        }

        public JavaClass getReturnType() {
            return get(returnType.getName());
        }

        public List<JavaClass> getParameters() {
            ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
            for (JavaType parameter : parameters) {
                result.add(get(parameter.getName()));
            }
            return result.build();
        }
    }

    public static final class JavaFieldBuilder extends JavaMemberBuilder<JavaField, JavaFieldBuilder> {
        private JavaType type;

        public JavaFieldBuilder withType(JavaType type) {
            this.type = type;
            return self();
        }

        public JavaClass getType() {
            return get(type.getName());
        }

        @Override
        JavaField construct(JavaFieldBuilder builder, ClassesByTypeName importedClasses) {
            return new JavaField(builder);
        }
    }

    public static final class JavaClassBuilder {
        private Optional<Source> source = Optional.absent();
        private JavaType javaType;
        private boolean isInterface;
        private Set<JavaModifier> modifiers = new HashSet<>();

        public JavaClassBuilder withSource(Source source) {
            this.source = Optional.of(source);
            return this;
        }

        @SuppressWarnings("unchecked")
        public JavaClassBuilder withType(JavaType javaType) {
            this.javaType = javaType;
            return this;
        }

        public JavaClassBuilder withInterface(boolean isInterface) {
            this.isInterface = isInterface;
            return this;
        }

        public JavaClassBuilder withModifiers(Set<JavaModifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public JavaClass build() {
            return new JavaClass(this);
        }

        public Optional<Source> getSource() {
            return source;
        }

        public JavaType getJavaType() {
            return javaType;
        }

        public boolean isInterface() {
            return isInterface;
        }

        public Set<JavaModifier> getModifiers() {
            return modifiers;
        }
    }

    public static class JavaAnnotationBuilder {
        private JavaType type;
        private Map<String, ValueBuilder> values = new HashMap<>();
        private ClassesByTypeName importedClasses;

        public JavaAnnotationBuilder withType(JavaType type) {
            this.type = type;
            return this;
        }

        public JavaType getJavaType() {
            return type;
        }

        public JavaAnnotationBuilder addProperty(String key, ValueBuilder valueBuilder) {
            values.put(key, valueBuilder);
            return this;
        }

        public JavaAnnotation build(ClassesByTypeName importedClasses) {
            this.importedClasses = importedClasses;
            return new JavaAnnotation(this);
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

        public abstract static class ValueBuilder {
            public abstract Optional<Object> build(ClassesByTypeName importedClasses);

            public static ValueBuilder ofFinished(final Object value) {
                return new ValueBuilder() {
                    @Override
                    public Optional<Object> build(ClassesByTypeName importedClasses) {
                        return Optional.of(value);
                    }
                };
            }

            public static ValueBuilder from(final JavaAnnotationBuilder builder) {
                return new ValueBuilder() {
                    @Override
                    public Optional<Object> build(ClassesByTypeName importedClasses) {
                        return Optional.<Object>of(builder.build(importedClasses));
                    }
                };
            }
        }
    }

    public static class JavaStaticInitializerBuilder extends JavaCodeUnitBuilder<JavaStaticInitializer, JavaStaticInitializerBuilder> {
        public JavaStaticInitializerBuilder() {
            withReturnType(JavaType.From.name(void.class.getName()));
            withParameters(Collections.<JavaType>emptyList());
            withName(JavaStaticInitializer.STATIC_INITIALIZER_NAME);
            withDescriptor("()V");
            withAnnotations(Collections.<JavaAnnotationBuilder>emptySet());
            withModifiers(Collections.<JavaModifier>emptySet());
        }

        @Override
        JavaStaticInitializer construct(JavaStaticInitializerBuilder builder, ClassesByTypeName importedClasses) {
            return new JavaStaticInitializer(builder);
        }
    }

    public interface BuilderWithBuildParameter<PARAMETER, VALUE> {
        VALUE build(PARAMETER parameter, ClassesByTypeName importedClasses);

        class BuildFinisher {
            public static <PARAMETER, VALUE> Set<VALUE> build(
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
}
