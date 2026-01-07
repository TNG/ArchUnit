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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.core.domain.JavaType.SignatureVisitor.Result.CONTINUE;
import static com.tngtech.archunit.core.domain.JavaType.SignatureVisitor.Result.STOP;
import static java.util.Collections.singleton;

/**
 * Represents a general Java type. This can e.g. be a class like {@code java.lang.String}, a parameterized type
 * like {@code List<String>} or a type variable like {@code T}.<br>
 * Besides having a {@link HasName#getName() name} and offering the possibility to being converted to an
 * {@link #toErasure() erasure} (which is then always {@link JavaClass a raw class object}) {@link JavaType} doesn't offer
 * an extensive API. Instead, users can check a {@link JavaType} for being an instance of a concrete subtype
 * (like {@link JavaTypeVariable}) and then cast it to the respective subclass
 * (same as with {@link Type} of the Java Reflection API).
 *
 * @see JavaClass
 * @see JavaParameterizedType
 * @see JavaTypeVariable
 * @see JavaWildcardType
 * @see JavaGenericArrayType
 */
@PublicAPI(usage = ACCESS)
public interface JavaType extends HasName {
    /**
     * Converts this {@link JavaType} into the erased type
     * (compare the <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6"> Java Language Specification</a>).
     * In particular this will result in
     * <ul>
     *     <li>the class itself, if this type is a {@link JavaClass}</li>
     *     <li>the {@link JavaClass} equivalent to {@link Object}, if this type is an unbound {@link JavaTypeVariable}</li>
     *     <li>the {@link JavaClass} equivalent to the erasure of the left most bound, if this type is a bound {@link JavaTypeVariable}</li>
     *     <li>if this type is a {@link JavaGenericArrayType}, the erasure will be the {@link JavaClass}
     *     equivalent to the array type that has the erasure of the generic component type of this type as its component type;
     *     e.g. take the generic array type {@code T[][]} where {@code T} is unbound, then the erasure will be the array type {@code Object[][]}</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    JavaClass toErasure();

    /**
     * Returns the set of all raw types that are involved in this type.
     * If this type is a {@link JavaClass}, then this method trivially returns only the class itself.
     * If this type is a {@link JavaParameterizedType}, {@link JavaTypeVariable}, {@link JavaWildcardType}, etc.,
     * then this method returns all raw types involved in type arguments and upper and lower bounds recursively.
     * If this type is an array type, then this method returns all raw types involved in the component type of the array type.
     * <br><br>
     * Examples:<br>
     * For the parameterized type
     * <pre><code>
     * List&lt;String&gt;</code></pre>
     * the result would be the {@link JavaClass classes} <code>[List, String]</code>.<br>
     * For the parameterized type
     * <pre><code>
     * Map&lt;? extends Serializable, List&lt;? super Integer[]&gt;&gt;</code></pre>
     * the result would be <code>[Map, Serializable, List, Integer]</code>.<br>
     * And for the type variable
     * <pre><code>
     * T extends List&lt;? super Integer&gt;</code></pre>
     * the result would be <code>[List, Integer]</code>.<br>
     * Thus, this method offers a quick way to determine all types a (possibly complex) type depends on.
     *
     * @return All raw types involved in this {@link JavaType}
     */
    @PublicAPI(usage = ACCESS)
    default Set<JavaClass> getAllInvolvedRawTypes() {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        traverseSignature(new SignatureVisitor() {
            @Override
            public Result visitClass(JavaClass type) {
                result.add(type.getBaseComponentType());
                return CONTINUE;
            }

            @Override
            public Result visitParameterizedType(JavaParameterizedType type) {
                result.add(type.toErasure());
                return CONTINUE;
            }
        });
        return result.build();
    }

    /**
     * Traverses through the signature of this {@link JavaType}.<br>
     * This method considers the type signature as a tree,
     * where e.g. a {@link JavaClass} is a simple leaf,
     * but a {@link JavaParameterizedType} has the type as root and then
     * branches out into its actual type arguments, which in turn can have type arguments
     * or upper/lower bounds in case of {@link JavaTypeVariable} or {@link JavaWildcardType}.<br>
     * The following is a simple visualization of such a signature tree:
     * <pre><code>
     * List&lt;Map&lt;? extends Serializable, String[]&gt;&gt;
     *                    |
     *    Map&lt;? extends Serializable, String[]&gt;
     *              /                   \
     *  ? extends Serializable         String[]
     *            |
     *      Serializable
     * </code></pre>
     * For every node visited the respective method of the provided {@code visitor}
     * will be invoked. The traversal happens depth first, i.e. in this case the {@code visitor}
     * would be invoked for all types down to {@code Serializable} before visiting the {@code String[]}
     * array type of the second branch. At every step it is possible to continue the traversal
     * by returning {@link SignatureVisitor.Result#CONTINUE CONTINUE} or stop at that point by
     * returning {@link SignatureVisitor.Result#STOP STOP}.<br><br>
     * Note that the traversal will continue to traverse bounds of type variables,
     * even if that type variable isn't declared in this signature itself.<br>
     * E.g. take the following scenario
     * <pre><code>
     * class Example&lt;T extends String&gt; {
     *     T field;
     * }</code></pre>
     * Traversing the {@link JavaField#getType() field type} of {@code field} will continue
     * down to the upper bounds of the type variable {@code T} and thus end at the type {@code String}.<br><br>
     * Also, note that the traversal will not continue down the type parameters of a raw type
     * declared in a signature.<br>
     * E.g. given the signature {@code class Example<T extends Map>} the traversal would stop at
     * {@code Map} and not traverse down the type parameters {@code K} and {@code V} of {@code Map}.
     *
     * @param visitor A {@link SignatureVisitor} to invoke for every encountered {@link JavaType}
     *                while traversing this signature.
     */
    @PublicAPI(usage = ACCESS)
    void traverseSignature(SignatureVisitor visitor);

    /**
     * @see #traverseSignature(SignatureVisitor)
     */
    @PublicAPI(usage = INHERITANCE)
    interface SignatureVisitor {
        default Result visitClass(JavaClass type) {
            return CONTINUE;
        }

        default Result visitParameterizedType(JavaParameterizedType type) {
            return CONTINUE;
        }

        default Result visitTypeVariable(JavaTypeVariable<?> type) {
            return CONTINUE;
        }

        default Result visitGenericArrayType(JavaGenericArrayType type) {
            return CONTINUE;
        }

        default Result visitWildcardType(JavaWildcardType type) {
            return CONTINUE;
        }

        /**
         * Result of a single step {@link #traverseSignature(SignatureVisitor) traversing a signature}.
         * After each step it's possible to either {@link #STOP stop} or {@link #CONTINUE continue}
         * the traversal.
         */
        @PublicAPI(usage = ACCESS)
        enum Result {
            /**
             * Causes the traversal to continue
             */
            @PublicAPI(usage = ACCESS)
            CONTINUE,
            /**
             * Causes the traversal to stop
             */
            @PublicAPI(usage = ACCESS)
            STOP
        }
    }

    /**
     * Predefined {@link ChainableFunction functions} to transform {@link JavaType}.
     */
    @PublicAPI(usage = ACCESS)
    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaType, JavaClass> TO_ERASURE = new ChainableFunction<JavaType, JavaClass>() {
            @Override
            public JavaClass apply(JavaType input) {
                return input.toErasure();
            }
        };
    }
}

class SignatureTraversal implements JavaType.SignatureVisitor {
    private final Set<JavaType> visited = new HashSet<>();
    private final JavaType.SignatureVisitor delegate;
    private Result lastResult;

    private SignatureTraversal(JavaType.SignatureVisitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public Result visitClass(JavaClass type) {
        // We only traverse type parameters of a JavaClass if the traversal was started *at the JavaClass* itself.
        // Otherwise, we can only encounter a regular class as a raw type in a type signature.
        // In these cases we don't want to traverse further down, as that would be surprising behavior
        // (consider `class MyClass<T extends Map>`, traversing into the type variables `K` and `V` of `Map` would be surprising).
        Supplier<Iterable<JavaTypeVariable<JavaClass>>> getFurtherTypesToTraverse = visited.isEmpty() ? type::getTypeParameters : Collections::emptyList;
        return visit(type, delegate::visitClass, getFurtherTypesToTraverse);
    }

    @Override
    public Result visitParameterizedType(JavaParameterizedType type) {
        return visit(type, delegate::visitParameterizedType, type::getActualTypeArguments);
    }

    @Override
    public Result visitTypeVariable(JavaTypeVariable<?> type) {
        return visit(type, delegate::visitTypeVariable, type::getUpperBounds);
    }

    @Override
    public Result visitGenericArrayType(JavaGenericArrayType type) {
        return visit(type, delegate::visitGenericArrayType, () -> singleton(type.getComponentType()));
    }

    @Override
    public Result visitWildcardType(JavaWildcardType type) {
        return visit(type, delegate::visitWildcardType, () -> Iterables.concat(type.getUpperBounds(), type.getLowerBounds()));
    }

    private <CURRENT extends JavaType, NEXT extends JavaType> Result visit(
            CURRENT type,
            Function<CURRENT, Result> visitCurrent,
            Supplier<Iterable<NEXT>> nextTypes
    ) {
        if (visited.contains(type)) {
            // if we've encountered this type already we continue traversing the siblings,
            // but we won't descend further into this type signature
            return setLast(CONTINUE);
        }
        visited.add(type);
        if (visitCurrent.apply(type) == CONTINUE) {
            Result result = visit(nextTypes.get());
            return setLast(result);
        } else {
            return setLast(STOP);
        }
    }

    private Result visit(Iterable<? extends JavaType> types) {
        for (JavaType nextType : types) {
            nextType.traverseSignature(this);
            if (lastResult == STOP) {
                return STOP;
            }
        }
        return CONTINUE;
    }

    private Result setLast(Result result) {
        lastResult = result;
        return result;
    }

    static SignatureTraversal from(JavaType.SignatureVisitor visitor) {
        return visitor instanceof SignatureTraversal ? (SignatureTraversal) visitor : new SignatureTraversal(visitor);
    }
}