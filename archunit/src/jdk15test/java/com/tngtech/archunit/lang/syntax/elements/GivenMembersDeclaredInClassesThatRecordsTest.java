package com.tngtech.archunit.lang.syntax.elements;

import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.core.domain.JavaMember;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersDeclaredInClassesThatTest.filterResultOf;
import static com.tngtech.archunit.testutil.Assertions.assertThatMembers;

public class GivenMembersDeclaredInClassesThatRecordsTest {

    @Test
    public void areRecords_predicate() {
        record SomeRecord(String param) {
        }

        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areRecords())
                .on(SomeRecord.class, Collection.class, Integer.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(SomeRecord.class);
    }

    @Test
    public void areNotRecords_predicate() {
        record SomeRecord(String param) {
        }

        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotRecords())
                .on(SomeRecord.class, Collection.class, Integer.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(Collection.class, Integer.class);
    }
}
