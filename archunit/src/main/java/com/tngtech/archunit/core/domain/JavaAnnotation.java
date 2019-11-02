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

import java.lang.annotation.Annotation;
import java.util.Map;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents an imported annotation on an annotated object like a class or a method. To be
 * independent of the classpath, all properties of this annotation are just stored as a simple
 * key value pairs. I.e. if you consider
 * <pre><code>
 *  {@literal @}MyAnnotation(name = "some name", anAttribute = 7)
 *  class MyClass {}
 * </code></pre>
 * this annotation will be imported storing the association
 * <pre><code>
 *   name --&gt; "some name"
 *   anAttribute --&gt; 7
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
 *   moreConvenient.anAttribute(); // --&gt; returns 7
 * </code></pre>
 * ----------<br>
 * NOTE<br>
 * ----------<br>
 * ArchUnit holds the annotation in a classpath independent representation, i.e. some types will
 * be mapped, when the access is proxied. Consider
 * <pre><code>
 *  {@literal @}SomeAnnotation(type = String.class)
 *  class MyClass {}
 * </code></pre>
 * Accesses to 'type' will be different for the proxied version:
 * <pre><code>
 *   someAnnotation.get("type"); // --&gt; returns JavaClass{String}
 *   someAnnotation.as(SomeAnnotation.class).type(); // --&gt; returns String.class
 * </code></pre>
 *
 * @param <OWNER> The type of the closest "parent" of this annotation. If this annotation
 *                is annotated on a class or member, it is that class or member. If this
 *                annotation is a member of another annotation, it is that annotation.
 */
public final class JavaAnnotation<OWNER extends HasDescription> implements HasType, HasOwner<OWNER>, HasDescription {
    private final JavaClass type;
    private final OWNER owner;
    private final CanBeAnnotated annotatedElement;
    private final String description;
    private final Map<String, Object> values;

    JavaAnnotation(OWNER owner, JavaAnnotationBuilder builder) {
        this.type = checkNotNull(builder.getType());
        this.owner = checkNotNull(owner);
        this.annotatedElement = getAnnotatedElement(owner);
        this.description = createDescription();
        this.values = checkNotNull(builder.getValues(this));
    }

    private CanBeAnnotated getAnnotatedElement(Object owner) {
        Object candiate = owner;
        while (!(candiate instanceof JavaClass) && !(candiate instanceof JavaMember) && (candiate instanceof HasOwner<?>)) {
            candiate = ((HasOwner<?>) candiate).getOwner();
        }
        if (!(candiate instanceof CanBeAnnotated)) {
            throw new IllegalArgumentException("Cannot derive annotated element from annotation owner: " + owner);
        }
        return (CanBeAnnotated) candiate;
    }

    private String createDescription() {
        CanBeAnnotated annotatedElement = getAnnotatedElement();
        String descriptionSuffix = annotatedElement instanceof HasDescription
                ? " on " + startWithLowerCase((HasDescription) annotatedElement)
                : "";
        return "Annotation <" + type.getName() + ">" + descriptionSuffix;
    }

    private String startWithLowerCase(HasDescription annotatedElement) {
        return annotatedElement.getDescription().substring(0, 1).toLowerCase() + annotatedElement.getDescription().substring(1);
    }

    /**
     * @deprecated Use {@link #getRawType()} instead
     */
    @Override
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public JavaClass getType() {
        return getRawType();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getRawType() {
        return type;
    }

    /**
     * Compare documentation of {@code OWNER} on {@link JavaAnnotation}
     */
    @Override
    public OWNER getOwner() {
        return owner;
    }

    /**
     * Returns either the element annotated with this {@link JavaAnnotation} (a class or member)
     * or in case this annotation is an annotation parameter, the element annotated with an
     * annotation that transitively declares this annotation as an annotation parameter.
     * <br><br>
     * Example:
     * <pre><code>
     * (1){@literal @}SomeAnnotation class SomeClass {}
     * (2) class SomeClass {
     *        {@literal @}SomeAnnotation SomeField someField;
     *     }
     * (3){@literal @}ComplexAnnotation(param = @SomeAnnotation) class SomeClass {}
     * </code></pre>
     * For case <code>(1)</code> the result of <code>someAnnotation.getAnnotatedElement()</code>
     * would be <code>SomeClass</code>, for case <code>(2)</code>
     * the result of <code>someAnnotation.getAnnotatedElement()</code> would be <code>someField</code>
     * and for <code>(3)</code> the result of <code>someAnnotation.getAnnotatedElement()</code> would
     * also be <code>SomeClass</code>, even though <code>@SomeAnnotation</code> is a parameter of
     * <code>@ComplexAnnotation</code>.
     * @return The closest element traversing up the tree, that can be annotated
     */
    @PublicAPI(usage = ACCESS)
    public CanBeAnnotated getAnnotatedElement() {
        return annotatedElement;
    }

    /**
     * Returns the value of the property with the given name, i.e. the result of the method with the property name
     * of the represented {@link java.lang.annotation.Annotation}. E.g. for
     * <pre><code>      {@literal @}SomeAnnotation(value = "someString", types = {SomeType.class, AnotherType.class})
     * class SomeAnnotatedClass {}
     * </code></pre>
     * the results will be
     * <pre><code>       someAnnotation.get("value") --&gt; "someString"
     * someAnnotation.get("types") --&gt; [JavaClass{SomeType}, JavaClass{AnotherType}]
     * </code></pre>
     *
     * @param property The name of the annotation property, i.e. the declared method name
     * @return the value of the given property, where the result type is more precisely
     * <ul>
     * <li>Class&lt;?&gt; --&gt; TypeDetails{clazz}</li>
     * <li>Class&lt;?&gt;[] --&gt; [TypeDetails{clazz},...]</li>
     * <li>Enum --&gt; JavaEnumConstant</li>
     * <li>Enum[] --&gt; [JavaEnumConstant,...]</li>
     * <li>anyOtherType --&gt; anyOtherType</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    public Optional<Object> get(String property) {
        return Optional.fromNullable(values.get(property));
    }

    /**
     * @return a map containing all [property --&gt; value], where each value correlates to {@link #get(String property)}
     */
    @PublicAPI(usage = ACCESS)
    public Map<String, Object> getProperties() {
        return values;
    }

    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> A as(Class<A> annotationType) {
        return AnnotationProxy.of(annotationType, this);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + type.getName() + '}';
    }
}
