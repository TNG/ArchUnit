package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.util.regex.Pattern.quote;

@RunWith(DataProviderRunner.class)
public class ShouldClassesThatRecordsTest {

    @Test
    @UseDataProvider(location = ShouldClassesThatTest.class, value = "no_classes_should_that_rule_starts")
    public void areRecords_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
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

    @Test
    @UseDataProvider(location = ShouldClassesThatTest.class, value = "no_classes_should_that_rule_starts")
    public void areNotRecords_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
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
