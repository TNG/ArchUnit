package com.tngtech.archunit.library;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.testclasses.standardstreams.UsesJavaLangIO;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;

public class GeneralCodingRulesNewerJavaVersionTest {

    @Test
    public void NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS_should_fail_on_java_lang_IO() {
        assertThatRule(NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS)
                .checking(new ClassFileImporter().importClasses(UsesJavaLangIO.class))
                .hasNumberOfViolations(5)
                .hasViolationContaining("Method <%s.useIo()> calls method <java.lang.IO.println(java.lang.Object)>",
                        UsesJavaLangIO.class.getName())
                .hasViolationContaining("Method <%s.useIo()> calls method <java.lang.IO.println()>",
                        UsesJavaLangIO.class.getName())
                .hasViolationContaining("Method <%s.useIo()> calls method <java.lang.IO.print(java.lang.Object)>",
                        UsesJavaLangIO.class.getName())
                .hasViolationContaining("Method <%s.useIo()> calls method <java.lang.IO.readln()>",
                        UsesJavaLangIO.class.getName())
                .hasViolationContaining("Method <%s.useIo()> calls method <java.lang.IO.readln(java.lang.String)>",
                        UsesJavaLangIO.class.getName());
    }
}
