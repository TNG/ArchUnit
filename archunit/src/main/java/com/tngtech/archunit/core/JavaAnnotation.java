package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaAnnotation {
    private final TypeDetails type;
    private final Map<String, Object> values;

    private JavaAnnotation(TypeDetails type, Map<String, Object> values) {
        this.type = checkNotNull(type);
        this.values = checkNotNull(values);
    }

    public TypeDetails getType() {
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

    static final Function<JavaAnnotation, String> GET_TYPE_NAME = new Function<JavaAnnotation, String>() {
        @Override
        public String apply(JavaAnnotation input) {
            return input.getType().getName();
        }
    };

    static class Builder {
        private TypeDetails type;
        private Map<String, Object> values = new HashMap<>();

        Builder withType(TypeDetails type) {
            this.type = type;
            return this;
        }

        Builder addProperty(String key, Object value) {
            values.put(key, value);
            return this;
        }

        JavaAnnotation build() {
            return new JavaAnnotation(type, values);
        }
    }
}
