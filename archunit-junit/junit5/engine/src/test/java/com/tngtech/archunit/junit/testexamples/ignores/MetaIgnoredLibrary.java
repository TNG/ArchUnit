package com.tngtech.archunit.junit.testexamples.ignores;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.testexamples.subtwo.SimpleRules;

@AnalyzeClasses(packages = "some.dummy.package")
public class MetaIgnoredLibrary {

    @ArchTest
    static final ArchTests unignored_lib_one = ArchTests.in(MetaIgnoredClass.class);

    @ArchTest
    static final ArchTests unignored_lib_two = ArchTests.in(MetaIgnoredMethod.class);

    @ArchTest
    @ArchIgnoreMetaAnnotation
    static final ArchTests ignored_lib = ArchTests.in(SimpleRules.class);

    public static final String UNIGNORED_LIB_ONE_FIELD = "unignored_lib_one";
    public static final String UNIGNORED_LIB_TWO_FIELD = "unignored_lib_two";
    public static final String IGNORED_LIB_FIELD = "ignored_lib";
}
