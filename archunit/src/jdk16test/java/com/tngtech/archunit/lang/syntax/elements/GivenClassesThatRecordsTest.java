package com.tngtech.archunit.lang.syntax.elements;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.elements.GivenClassesThatTest.filterResultOf;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;

public class GivenClassesThatRecordsTest {

    @Test
    public void areRecords_predicate() {
        record SomeRecord(String param) {
        }

        List<JavaClass> classes = filterResultOf(classes().that().areRecords())
                .on(SomeRecord.class, String.class, Integer.class);

        assertThatTypes(classes).matchInAnyOrder(SomeRecord.class);
    }

    @Test
    public void areNotRecords_predicate() {
        record SomeRecord(String param) {
        }

        List<JavaClass> classes = filterResultOf(classes().that().areNotRecords())
                .on(SomeRecord.class, String.class, Integer.class);

        assertThatTypes(classes).matchInAnyOrder(String.class, Integer.class);
    }
}
