package com.tngtech.archunit.junit.testexamples.ignores;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.subtwo.SimpleRules;

@AnalyzeClasses(packages = "some.dummy.package")
public class IgnoredLibrary {

    @ArchTest
    static final ArchRules unignored_lib_one = ArchRules.in(IgnoredClass.class);

    @ArchTest
    static final ArchRules unignored_lib_two = ArchRules.in(IgnoredMethod.class);

    @ArchTest
    @ArchIgnore
    static final ArchRules ignored_lib = ArchRules.in(SimpleRules.class);

    public static final String UNIGNORED_LIB_ONE_FIELD = "unignored_lib_one";
    public static final String UNIGNORED_LIB_TWO_FIELD = "unignored_lib_two";
    public static final String IGNORED_LIB_FIELD = "ignored_lib";
}
