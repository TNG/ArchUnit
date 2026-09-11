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
package com.tngtech.archunit.core.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.Optional;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * {@code JavaAnnotatedType} represents the potentially annotated use of a {@link JavaType}.</br>
 * The use may be of any type in the Java programming language, including an array type, a parameterized type, a type variable, or a wildcard type.</br>
 * Note that any annotations returned by methods on this interface are type annotations (annotations with {@code @}{@link Target}{@code (}{@link ElementType#TYPE_USE}{@code )}) as the entity being potentially annotated is a type.
 * If you are looking for annotations of the classes, methods, fields, etc. you must search for them on the {@link JavaClass}, {@link JavaMethod}, {@link JavaField}, etc. </br>
 * If you get the {@code AnnotatedType} of something which does not have type annotations, it is still an {@code AnnotatedType} - just one without annotations.
 * Iterating into type parameters, array components, etc. can still yield type annotations on nested type usages.
 * {@code TYPE_USE} annotations are usually used for extra constraints or explicit information about some type usage, most often nullability (e.g. {@code @Nullable} and {@code @NonNull} from <a href=https://jspecify.dev/docs/api/org/jspecify/annotations/package-summary.html>jspecify</a>
 * </p>
 * {@code TYPE_USE} annotations may look in some places exactly like annotations of other target types, but it is important to distinguish between annotations on members and their type.
 * They can also be used in places which define types (exactly like annotations with{@code @}{@link Target}{@code (}{@link ElementType#TYPE}{@code )} or {@code @}{@link Target}{@code (}{@link ElementType#TYPE_PARAMETER}{@code )}),
 * in which case the annotations is NOT contained in the {@code AnnotatedType} but on the {@link JavaClass} (potentially an interface) or {@link JavaTypeVariable} instead. The type definition only uses the type to define it, so it makes no sense to separate these concepts in this place.</br>
 * If an annotation has multiple target types, it is also possible that a single annotation in code shows up in multiple locations.
 * </p>
 * Multiple variants of type usages involve nesting different types into other types. Each nested use of a type can itself be annotated. </br>
 * For more details see:
 * <ul>
 *     <li>{@link JavaAnnotatedArrayType} for array types, e.g. {@code String[]}, {@code Collection<?>[]}</li>
 *     <li>{@link JavaAnnotatedParameterizedType} for type parameters, e.g. {@code List<String>}</li>
 *     <li>{@link JavaAnnotatedTypeVariable} for declarations and usages of type variables, e.g. {@code T} from {@code MyClass<T>} or method type parameter</li>
 *     <li>{@link JavaAnnotatedWildcardType} for wildcard usages, e.g. {@code List<? extends Serializable>} where both the wildcard and the bounds can be annotated</li>
 *     <li>{@link #getAnnotatedEnclosingType()} for nested type usages, e.g. {@code Outer.Inner} where both the inner and the outer type can be annotated</li>
 * </ul>
 * </p>
 * Examples:
 * <ul>
 *     <li> {@code @FieldAnnotation @TypeUseAnnotation String field;} has exactly {@code @FieldAnnotation} on the {@link JavaField} and exactly {@code @TypeUseAnnotation} on the {@code AnnotatedType} of the field.</li>
 *     <li> {@code String method(@FieldAnnotation @TypeUseAnnotation String param);} has exactly {@code @FieldAnnotation} on the {@link JavaParameter} and exactly {@code @TypeUseAnnotation} on the {@code AnnotatedType} of the parameter.</li>
 *     <li> {@code @FieldAndTypeUseAnnotation String field;} has exactly {@code @FieldAndTypeUseAnnotation} on both the {@link JavaField} and on the {@code AnnotatedType} of the field ({@code @FieldAndTypeUseAnnotation} is meta-annotated with <code>@Target({ElementType.FIELD, ElementType.TYPE_USE})</code>).</li>
 *     <li> {@code @TypeAnnotation @TypeUseAnnotation class Clazz} has both annotations directly the class.</li>
 *     <li> {@code class Clazz<@TypeParamAnnotation @TypeUseAnnotation T} has both annotations directly on the {@link JavaTypeVariable}<code> T</code>.</li>
 *     <li> {@code class Clazz<T extends @TypeUseAnnotation Serializable} has exactly {@code @FieldAndTypeUseAnnotation} on the type bound of the {@link JavaTypeVariable}<code> T</code>.</li>
 * </ul>
 *
 * @see AnnotatedType for the equivalent reflection representation
 */
@PublicAPI(usage = ACCESS)
public interface JavaAnnotatedType extends HasAnnotations<JavaAnnotatedType> {
    // TODO should this interface also have an owner? for top level element, the owner is the field, method RETURN??, method throw clause??
    //  there might be cases where we have currently no representation of the owner of the type (e.g. local variable?). so the JavaMethod might win over it's parameter? not totally clear

    /**
     * @return {@link JavaType} used by this {@code JavaAnnotatedType} without any type annotation information
     */
    @PublicAPI(usage = ACCESS)
    JavaType getType();

    /**
     * If {@code this} {@code AnnotatedType} represents the usage of a nested type, usages of both the outer and the inner class can be annotated. Use this method to get annotations on the outer type.</br>
     * Example: In the type {@code @A1 Outer.@A2 Inner} where {@code this} represents {@code @A2 Inner}, the return value of this method contains {@code @A1 Outer}.
     * @return annotated usage of outer class used to access the current nested type
     * @see AnnotatedType#getAnnotatedOwnerType() for equivalent method from reflection API.
     */
    @PublicAPI(usage = ACCESS)
    Optional<JavaAnnotatedType> getAnnotatedEnclosingType();
}
