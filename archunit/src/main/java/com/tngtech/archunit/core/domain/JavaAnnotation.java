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
package com.tngtech.archunit.core.domain;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Optional;
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
 */
public final class JavaAnnotation implements HasType {
    private final JavaClass type;
    private final Map<String, Object> values;

    JavaAnnotation(JavaAnnotationBuilder builder) {
        this.type = checkNotNull(builder.getType());
        this.values = checkNotNull(builder.getValues());
    }

    /**
     * @deprecated Use {@link #getRawType()} instead
     */
    @Override
    @Deprecated
    public JavaClass getType() {
        return getRawType();
    }

    @Override
    public JavaClass getRawType() {
        return type;
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
}
