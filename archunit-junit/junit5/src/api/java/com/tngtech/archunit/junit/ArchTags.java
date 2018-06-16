package com.tngtech.archunit.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.Internal;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Simply a container for {@link ArchTag}. Should never be used directly, but instead
 * {@link ArchTag} should be used in a {@linkplain Repeatable repeatable} manner, e.g.
 * <br><br>
 * <pre><code>
 *{@literal @}ArchTag("foo")
 *{@literal @}ArchTag("bar")
 * static ArchRule example = classes()...
 * </code></pre>
 */
@Internal
@Inherited
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD})
public @interface ArchTags {
    ArchTag[] value();
}
