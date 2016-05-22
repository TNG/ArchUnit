package com.tngtech.archunit.core;

import com.tngtech.archunit.core.MemberDescription.ForConstructor;
import com.tngtech.archunit.core.MemberDescription.ForDeterminedField;
import com.tngtech.archunit.core.MemberDescription.ForDeterminedMember;
import com.tngtech.archunit.core.MemberDescription.ForDeterminedMethod;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MemberDescriptionTest {
    @Test
    public void equals_and_hashcode_for_determined_fields() throws Exception {
        ForDeterminedMember<?> member = new ForDeterminedField(SomeClass.class.getDeclaredField("someIntField"));
        ForDeterminedMember<?> equalMember = new ForDeterminedField(SomeClass.class.getDeclaredField("someIntField"));
        ForDeterminedMember<?> differentMember = new ForDeterminedField(SomeClass.class.getDeclaredField("someStringField"));

        assertThat(equalMember).isEqualTo(member);
        assertThat(equalMember.hashCode()).as("hashcode").isEqualTo(member.hashCode());
        assertThat(differentMember).isNotEqualTo(member);
    }

    @Test
    public void equals_and_hashcode_for_determined_methods() throws Exception {
        ForDeterminedMember<?> member = new ForDeterminedMethod(SomeClass.class.getDeclaredMethod("someMethod"));
        ForDeterminedMember<?> equalMember = new ForDeterminedMethod(SomeClass.class.getDeclaredMethod("someMethod"));
        ForDeterminedMember<?> differentMember = new ForDeterminedMethod(SomeClass.class.getDeclaredMethod("someOtherMethod"));

        assertThat(equalMember).isEqualTo(member);
        assertThat(equalMember.hashCode()).as("hashcode").isEqualTo(member.hashCode());
        assertThat(differentMember).isNotEqualTo(member);
    }

    @Test
    public void equals_and_hashcode_for_determined_constructors() throws Exception {
        ForDeterminedMember<?> member = new ForConstructor(SomeClass.class.getDeclaredConstructor());
        ForDeterminedMember<?> equalMember = new ForConstructor(SomeClass.class.getDeclaredConstructor());
        ForDeterminedMember<?> differentMember = new ForConstructor(SomeClass.class.getDeclaredConstructor(int.class));

        assertThat(equalMember).isEqualTo(member);
        assertThat(equalMember.hashCode()).as("hashcode").isEqualTo(member.hashCode());
        assertThat(differentMember).isNotEqualTo(member);
    }

    private static class SomeClass {
        int someIntField;
        String someStringField;

        SomeClass() {
        }

        SomeClass(int integer) {
        }

        void someMethod() {
        }

        void someOtherMethod() {
        }
    }
}