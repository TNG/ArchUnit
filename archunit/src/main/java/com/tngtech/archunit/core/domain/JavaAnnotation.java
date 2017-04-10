package com.tngtech.archunit.core.domain;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an imported annotation on an annotated object like a class or a method. To be
 * independent of the classpath, all properties of this annotation are just stored as a simple
 * key value pairs. I.e. if you consider
 * <pre><code>
 *  {@literal @}MyAnnotation(name = "some name", anAttribute = 7)
 *   class MyClass {}
 * </code></pre>
 * this annotation will be imported storing the association
 * <pre><code>
 *   name -> "some name"
 *   anAttribute -> 7
 * </code></pre>
 * Properties will be made available via {@link #get(String)}, e.g.
 * <pre><code>
 *   myAnnotation.get("anAttribute")
 * </code></pre>
 * will return the value 7. Since this behavior is inconvenient (loss of type safety),
 * there is another approach to retrieve these values, if the annotation can
 * be resolved on the classpath. It's then possible to access a simple proxy
 * <pre><code>
 *   MyAnnotation moreConvenient = myAnnotation.as(MyAnnotation.class);
 *   moreConvenient.anAttribute(); // -> returns 7
 * </code></pre>
 * ----------<br>
 * NOTE<br>
 * ----------<br>
 * ArchUnit holds the annotation in a classpath independent representation, i.e. some types will
 * be mapped, when the access is proxied. Consider
 * <pre><code>
 *  {@literal @}SomeAnnotation(type = String.class)
 *   class MyClass {}
 * </code></pre>
 * Accesses to 'type' will be different for the proxied version:
 * <pre><code>
 *   someAnnotation.get("type"); // -> returns JavaClass{String}
 *   someAnnotation.as(SomeAnnotation.class).type(); // -> returns String.class
 * </code></pre>
 */
public class JavaAnnotation implements HasType {
    private final JavaClass type;
    private final Map<String, Object> values;

    public JavaAnnotation(DomainBuilders.JavaAnnotationBuilder builder) {
        this.type = checkNotNull(builder.getType());
        this.values = checkNotNull(builder.getValues());
    }

    @Override
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
     * someAnnotation.get("types") -> [JavaClass{SomeType}, JavaClass{AnotherType}]
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

}
