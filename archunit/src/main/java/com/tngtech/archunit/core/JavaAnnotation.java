package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaAnnotation {
    private final JavaClass type;
    private final Map<String, Object> values;

    private JavaAnnotation(JavaClass type, Map<String, Object> values) {
        this.type = checkNotNull(type);
        this.values = checkNotNull(values);
    }

    public JavaClass getType() {
        return type;
    }

    /**
     * Returns the value of the property with the given name, i.e. the result of the method with the property name
     * of the represented {@link java.lang.annotation.Annotation}. E.g. for
     * <pre><code>      {@literal @}SomeAnnotation(value = "someString", types = {SomeType.class, AnotherType.class})
     * class SomeAnnotatedClass {}
     * </code></pre>
     * the results will be
     * <pre><code>       someAnnotation.get("value") -> "someString"
     * someAnnotation.get("types") -> [TypeDetails{SomeType}, TypeDetails{AnotherType}]
     * </code></pre>
     *
     * @param property The name of the annotation property, i.e. the declared method name
     * @return the value of the given property, where the result type is more precisely
     * <ul>
     * <li>Class&lt;?&gt; -> TypeDetails{clazz}</li>
     * <li>Class&lt;?&gt;[] -> [TypeDetails{clazz},...]</li>
     * <li>Enum -> JavaEnumConstant</li>
     * <li>Enum[] -> [JavaEnumConstant,...]</li>
     * <li>anyOtherType -> anyOtherType</li>
     * </ul>
     */
    public Optional<Object> get(String property) {
        return Optional.fromNullable(values.get(property));
    }

    /**
     * @return a map containing all [property -> value], where each value correlates to {@link #get(String property)}
     */
    public Map<String, Object> getProperties() {
        return values;
    }

    public <A extends Annotation> A as(Class<A> annotationType) {
        return AnnotationProxy.of(annotationType, this);
    }

    static Map<String, JavaAnnotation> buildAnnotations(Set<Builder> annotations, ImportedClasses.ByTypeName importedClasses) {
        ImmutableMap.Builder<String, JavaAnnotation> result = ImmutableMap.builder();
        for (Builder annotationBuilder : annotations) {
            JavaAnnotation javaAnnotation = annotationBuilder.build(importedClasses);
            result.put(javaAnnotation.getType().getName(), javaAnnotation);
        }
        return result.build();
    }

    static class Builder {
        private JavaType type;
        private Map<String, ValueBuilder> values = new HashMap<>();
        private ImportedClasses.ByTypeName importedClasses;

        Builder withType(JavaType type) {
            this.type = type;
            return this;
        }

        JavaType getType() {
            return type;
        }

        Builder addProperty(String key, ValueBuilder valueBuilder) {
            values.put(key, valueBuilder);
            return this;
        }

        JavaAnnotation build(ImportedClasses.ByTypeName importedClasses) {
            this.importedClasses = importedClasses;
            return new JavaAnnotation(createFinalType(), getValues(importedClasses));
        }

        private JavaClass createFinalType() {
            return importedClasses.get(type.getName());
        }

        private Map<String, Object> getValues(ImportedClasses.ByTypeName importedClasses) {
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

        private void addDefaultValues(ImmutableMap.Builder<String, Object> result, ImportedClasses.ByTypeName importedClasses) {
            for (JavaMethod method : importedClasses.get(type.getName()).getMethods()) {
                if (!values.containsKey(method.getName()) && method.getDefaultValue().isPresent()) {
                    result.put(method.getName(), method.getDefaultValue().get());
                }
            }
        }
    }

    static abstract class ValueBuilder {
        abstract Optional<Object> build(ImportedClasses.ByTypeName importedClasses);

        static ValueBuilder ofFinished(final Object value) {
            return new ValueBuilder() {
                @Override
                Optional<Object> build(ImportedClasses.ByTypeName importedClasses) {
                    return Optional.of(value);
                }
            };
        }

        static ValueBuilder from(final JavaAnnotation.Builder builder) {
            return new ValueBuilder() {
                @Override
                Optional<Object> build(ImportedClasses.ByTypeName importedClasses) {
                    return Optional.<Object>of(builder.build(importedClasses));
                }
            };
        }
    }
}
