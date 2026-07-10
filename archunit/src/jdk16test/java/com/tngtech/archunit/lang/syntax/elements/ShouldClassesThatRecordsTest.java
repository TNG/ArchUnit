package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.util.regex.Pattern.quote;

public class ShouldClassesThatRecordsTest {

    @ParameterizedTest
    @MethodSource("com.tngtech.archunit.lang.syntax.elements.ShouldClassesThatTest#no_classes_should_that_rule_starts")
    void areRecords_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        record SomeRecord(String param) {
        }
        @SuppressWarnings("unused")
        class ClassAccessingRecord {
            void call() {
                new SomeRecord("foo");
            }
        }
        @SuppressWarnings("unused")
        class ClassNotAccessingRecord {
            void call() {
                new ClassAccessingRecord();
            }
        }

        assertThatRule(noClassesShouldThatRuleStart.areRecords())
                .checking(new ClassFileImporter().importClasses(ClassAccessingRecord.class, SomeRecord.class, ClassNotAccessingRecord.class))
                .hasViolationMatching(".*" + quote(ClassAccessingRecord.class.getName()) + ".* calls constructor .*" + quote(SomeRecord.class.getName()) + ".*")
                .hasNoViolationMatching(".*" + quote(ClassNotAccessingRecord.class.getName()) + ".*" + quote(ClassAccessingRecord.class.getName()) + ".*");
    }

    @ParameterizedTest
    @MethodSource("com.tngtech.archunit.lang.syntax.elements.ShouldClassesThatTest#no_classes_should_that_rule_starts")
    void areNotRecords_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        record SomeRecord(String param) {
        }
        @SuppressWarnings("unused")
        class ClassAccessingRecord {
            void call() {
                new SomeRecord("foo");
            }
        }
        @SuppressWarnings("unused")
        class ClassNotAccessingRecord {
            void call() {
                new ClassAccessingRecord();
            }
        }

        assertThatRule(noClassesShouldThatRuleStart.areNotRecords())
                .checking(new ClassFileImporter().importClasses(ClassAccessingRecord.class, SomeRecord.class, ClassNotAccessingRecord.class))
                .hasViolationMatching(".*" + quote(ClassNotAccessingRecord.class.getName()) + ".* calls constructor .*" + quote(ClassAccessingRecord.class.getName()) + ".*")
                .hasNoViolationMatching(".*" + quote(ClassAccessingRecord.class.getName()) + ".*" + quote(SomeRecord.class.getName()) + ".*");
    }
}
