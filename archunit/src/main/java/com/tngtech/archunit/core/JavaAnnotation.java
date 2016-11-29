package com.tngtech.archunit.core;

import java.util.Map;

public class JavaAnnotation implements HasOwner<JavaMember<?, ?>> {
    private final TypeDetails type;
    private final Map<String, Object> values;
    private final JavaMember<?, ?> owner;

    private JavaAnnotation(Builder builder) {
        type = builder.type;
        values = builder.values;
        owner = builder.owner;
    }

    public TypeDetails getType() {
        return type;
    }

    @Override
    public JavaMember<?, ?> getOwner() {
        return owner;
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
    public Object get(String property) {
        return values.get(property);
    }

    /**
     * @return a map containing all [property -> value], where each value is derived via {@link #get(String property)}
     */
    public Map<String, Object> getProperties() {
        return values;
    }

    static class Builder implements BuilderWithBuildParameter<JavaMember<?, ?>, JavaAnnotation> {
        private TypeDetails type;
        private Map<String, Object> values;
        private JavaMember<?, ?> owner;

        Builder withType(TypeDetails type) {
            this.type = type;
            return this;
        }

        Builder withValues(Map<String, Object> values) {
            this.values = values;
            return this;
        }

        @Override
        public JavaAnnotation build(JavaMember<?, ?> owner) {
            this.owner = owner;
            return new JavaAnnotation(this);
        }
    }
}
