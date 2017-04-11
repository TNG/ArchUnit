package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ClassesTransformer;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * A {@link ClassesTransformer} that simply returns the supplied collection of {@link JavaClass}
 * (i.e. the identity transformation)
 */
public final class ClassesIdentityTransformer extends AbstractClassesTransformer<JavaClass> {
    private ClassesIdentityTransformer() {
        super("classes");
    }

    /**
     * @see ClassesIdentityTransformer
     */
    @PublicAPI(usage = ACCESS)
    public static ClassesTransformer<JavaClass> classes() {
        return new ClassesIdentityTransformer();
    }

    @Override
    public Iterable<JavaClass> doTransform(JavaClasses collection) {
        return collection;
    }
}
