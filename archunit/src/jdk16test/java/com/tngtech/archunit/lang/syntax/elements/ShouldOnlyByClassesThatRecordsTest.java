package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.util.regex.Pattern.quote;

@RunWith(DataProviderRunner.class)
public class ShouldOnlyByClassesThatRecordsTest {

    @Test
    @UseDataProvider(location = ShouldOnlyByClassesThatTest.class, value = "should_only_be_by_rule_starts")
    public void areRecords_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
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

    @Test
    @UseDataProvider(location = ShouldOnlyByClassesThatTest.class, value = "should_only_be_by_rule_starts")
    public void areNotRecords_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
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
