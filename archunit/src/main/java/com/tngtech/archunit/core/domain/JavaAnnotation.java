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
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

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
 *   name &rarr; "some name"
 *   anAttribute &rarr; 7
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
 *   moreConvenient.anAttribute(); // &rarr; returns 7
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
 *   someAnnotation.get("type"); // &rarr; returns JavaClass{String}
 *   someAnnotation.as(SomeAnnotation.class).type(); // &rarr; returns String.class
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
     * <pre><code>       someAnnotation.get("value") &rarr; "someString"
     * someAnnotation.get("types") &rarr; [JavaClass{SomeType}, JavaClass{AnotherType}]
     * </code></pre>
     *
     * @param property The name of the annotation property, i.e. the declared method name
     * @return the value of the given property, where the result type is more precisely
     * <ul>
     * <li>Class&lt;?&gt; &rarr; JavaClass{clazz}</li>
     * <li>Class&lt;?&gt;[] &rarr; [JavaClass{clazz},...]</li>
     * <li>Enum &rarr; JavaEnumConstant</li>
     * <li>Enum[] &rarr; [JavaEnumConstant,...]</li>
     * <li>Annotation &rarr; JavaAnnotation</li>
     * <li>Annotation[] &rarr; [JavaAnnotation,...]</li>
     * <li>anyOtherType &rarr; anyOtherType</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    public Optional<Object> get(String property) {
        return Optional.fromNullable(values.get(property));
    }

    /**
     * @return a map containing all [property &rarr; value], where each value correlates to {@link #get(String property)}
     */
    @PublicAPI(usage = ACCESS)
    public Map<String, Object> getProperties() {
        return values;
    }

    /**
     * Simple implementation of the Visitor pattern (compare e.g.
     * <a href="https://en.wikipedia.org/wiki/Visitor_pattern">https://en.wikipedia.org/wiki/Visitor_pattern</a>).<br><br>
     * While it is fairly convenient to analyse a {@link JavaAnnotation} that is on the classpath (e.g. by using {@link #as(Class)}),
     * it is quite tedious to do so for a {@link JavaAnnotation} not on the classpath (i.e. via {@link #getProperties()}).<br><br>
     * {@link #accept(ParameterVisitor)} offers an alternative by taking away traversal and casting logic when analysing
     * potentially unknown {@link JavaAnnotation JavaAnnotations} of different parameter structures.<br>
     * Whether using this method or performing casting operations on {@link #getProperties()} might depend on the use case.
     * For a known annotation where only a single known parameter is relevant, a solution like<br><br>
     * <pre><code>
     * String value = (String) knownJavaAnnotation.get("value").get()
     * </code></pre>
     *
     * might be completely sufficient. However an analysis like "all class parameters of all annotations in package
     * <code>foo.bar</code> should implement a certain interface" (or potentially nested annotations), this might not be an easily
     * readable approach. {@link #accept(ParameterVisitor)} makes this use case fairly trivial:<br><br>
     *
     * <pre><code>
     * unknownJavaAnnotation.accept(new DefaultParameterVisitor() {
     *    {@literal @}Override
     *     public void visitClass(String propertyName, JavaClass javaClass) {
     *         // do whatever check on the class parameter javaClass
     *     }
     * });</code></pre>
     *
     * Furthermore {@link ParameterVisitor} does exactly specify which cases can occur for {@link JavaAnnotation} parameters
     * without the need to introspect and cast the values. In case traversal into nested {@link JavaAnnotation JavaAnnotations} is necessary,
     * this also becomes quite simple:<br><br>
     *
     * <pre><code>
     * unknownJavaAnnotation.accept(new DefaultParameterVisitor() {
     *     // parameter handling logic
     *
     *    {@literal @}Override
     *     public void visitAnnotation(String propertyName, JavaAnnotation&lt;?&gt; nestedAnnotation) {
     *         nestedAnnotation.accept(this);
     *     }
     * });</code></pre>
     *
     * @param parameterVisitor A visitor which allows to implement behavior for different types of annotation parameters
     */
    @PublicAPI(usage = ACCESS)
    public void accept(ParameterVisitor parameterVisitor) {
        JavaAnnotationParameterVisitorAcceptor.accept(getProperties(), parameterVisitor);
    }

    /**
     * Returns a compile safe proxied version of the respective {@link JavaAnnotation}. In other words, the result
     * of <code>as(MyAnnotation.class)</code> will be of type <code>MyAnnotation</code> and allow property access
     * in a compile safe manner. For this to work the respective <code>{@link Annotation}</code> including all
     * referred parameter types must be on the classpath or an {@link Exception} will be thrown.
     * Furthermore the respective {@link JavaAnnotation} must actually be an import of the passed parameter
     * <code>annotationType</code> or a {@link RuntimeException} will likely occur.
     *
     * @param annotationType Any type implementing {@link Annotation}
     * @param <A> The type of the imported {@link Annotation} backing this {@link JavaAnnotation}
     * @return A compile safe proxy of type {@link A}
     */
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

    /**
     * A Visitor (compare {@link #accept(ParameterVisitor)}) offering possibilities to specify
     * behavior when various types of {@link JavaAnnotation#getProperties()} are encountered.<br><br>
     * The list of declared methods is exhaustive, thus any legal parameter type of an {@link Annotation}
     * is represented by the respective <code>visit</code>-method.
     */
    @PublicAPI(usage = INHERITANCE)
    public interface ParameterVisitor {
        void visitBoolean(String propertyName, boolean propertyValue);

        void visitByte(String propertyName, byte propertyValue);

        void visitCharacter(String propertyName, Character propertyValue);

        void visitDouble(String propertyName, Double propertyValue);

        void visitFloat(String propertyName, Float propertyValue);

        void visitInteger(String propertyName, int propertyValue);

        void visitLong(String propertyName, Long propertyValue);

        void visitShort(String propertyName, Short propertyValue);

        void visitString(String propertyName, String propertyValue);

        void visitClass(String propertyName, JavaClass propertyValue);

        void visitEnumConstant(String propertyName, JavaEnumConstant propertyValue);

        void visitAnnotation(String propertyName, JavaAnnotation<?> propertyValue);
    }

    /**
     * Default implementation of {@link ParameterVisitor} implementing a no-op
     * behavior, i.e. this Visitor will do nothing on any type encountered.<br>
     * <code>visit</code>-methods for relevant types can be selectively overridden
     * (compare {@link #accept(ParameterVisitor)}).
     */
    @PublicAPI(usage = INHERITANCE)
    public static class DefaultParameterVisitor implements ParameterVisitor {
        @Override
        public void visitBoolean(String propertyName, boolean propertyValue) {
        }

        @Override
        public void visitByte(String propertyName, byte propertyValue) {
        }

        @Override
        public void visitCharacter(String propertyName, Character propertyValue) {
        }

        @Override
        public void visitDouble(String propertyName, Double propertyValue) {
        }

        @Override
        public void visitFloat(String propertyName, Float propertyValue) {
        }

        @Override
        public void visitInteger(String propertyName, int propertyValue) {
        }

        @Override
        public void visitLong(String propertyName, Long propertyValue) {
        }

        @Override
        public void visitShort(String propertyName, Short propertyValue) {
        }

        @Override
        public void visitString(String propertyName, String propertyValue) {
        }

        @Override
        public void visitClass(String propertyName, JavaClass propertyValue) {
        }

        @Override
        public void visitEnumConstant(String propertyName, JavaEnumConstant propertyValue) {
        }

        @Override
        public void visitAnnotation(String propertyName, JavaAnnotation<?> propertyValue) {
        }
    }
}
