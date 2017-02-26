package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ClassesTransformer;

/**
 * A {@link ClassesTransformer} that simply returns the supplied collection of {@link JavaClass}
 * (i.e. the identity transformation)
 */
public class ClassesIdentityTransformer extends AbstractClassesTransformer<JavaClass> {
    private ClassesIdentityTransformer() {
        super("classes");
    }

    /**
     * @see ClassesIdentityTransformer
     */
    public static ClassesTransformer<JavaClass> classes() {
        return new ClassesIdentityTransformer();
    }

    @Override
    public Iterable<JavaClass> doTransform(JavaClasses collection) {
        return collection;
    }
}
