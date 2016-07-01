package com.tngtech.archunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.ClassFileImporter.ImportOption;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies which packages should be scanned and tested when running a test via the {@link ArchUnitRunner}.
 *
 * @see ArchUnitRunner
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface AnalyseClasses {
    String[] packages() default {};

    Class[] packagesOf() default {};

    Class<? extends ImportOption> importOption() default ImportOption.Everything.class;
}
