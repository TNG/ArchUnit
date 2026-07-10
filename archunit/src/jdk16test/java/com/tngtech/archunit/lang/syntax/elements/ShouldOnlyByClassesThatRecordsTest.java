package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.util.regex.Pattern.quote;

public class ShouldOnlyByClassesThatRecordsTest {

    @ParameterizedTest
    @MethodSource("com.tngtech.archunit.lang.syntax.elements.ShouldOnlyByClassesThatTest#should_only_be_by_rule_starts")
    void areRecords_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        record SomeRecord(String param) {
        }
        record RecordAccessingRecord(String param) {
            @SuppressWarnings("unused")
            void call() {
                new SomeRecord("foo");
            }
        }
        @SuppressWarnings("unused")
        class ClassAccessingRecord {
            void call() {
                new SomeRecord("bar");
            }
        }

        assertThatRule(classesShouldOnlyBeBy.areRecords())
                .checking(new ClassFileImporter().importClasses(RecordAccessingRecord.class, SomeRecord.class, ClassAccessingRecord.class))
                .hasViolationMatching(".*" + quote(ClassAccessingRecord.class.getName()) + ".* calls constructor .*" + quote(SomeRecord.class.getName()) + ".*")
                .hasNoViolationMatching(".*" + quote(RecordAccessingRecord.class.getName()) + ".*" + quote(SomeRecord.class.getName()) + ".*");
    }

    @ParameterizedTest
    @MethodSource("com.tngtech.archunit.lang.syntax.elements.ShouldOnlyByClassesThatTest#should_only_be_by_rule_starts")
    void areNotRecords_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        record SomeRecord(String param) {
        }
        record RecordAccessingRecord(String param) {
            @SuppressWarnings("unused")
            void call() {
                new SomeRecord("foo");
            }
        }
        @SuppressWarnings("unused")
        class ClassAccessingRecord {
            void call() {
                new SomeRecord("bar");
            }
        }

        assertThatRule(classesShouldOnlyBeBy.areNotRecords())
                .checking(new ClassFileImporter().importClasses(RecordAccessingRecord.class, SomeRecord.class, ClassAccessingRecord.class))
                .hasViolationMatching(".*" + quote(RecordAccessingRecord.class.getName()) + ".* calls constructor .*" + quote(SomeRecord.class.getName()) + ".*")
                .hasNoViolationMatching(".*" + quote(ClassAccessingRecord.class.getName()) + ".*" + quote(SomeRecord.class.getName()) + ".*");
    }
}
