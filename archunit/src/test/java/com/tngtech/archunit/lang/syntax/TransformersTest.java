package com.tngtech.archunit.lang.syntax;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class TransformersTest {
    @Test
    public void test_classes() {
        JavaClasses input = importClasses(Object.class, String.class);

        DescribedIterable<JavaClass> output = Transformers.classes().transform(input);

        assertThat(output).containsOnlyElementsOf(input);
    }

    @DataProvider
    public static Object[][] members_testcases() {
        return $$(
                $(Transformers.members(),
                        Sets.union(
                                createMemberStrings(ClassWithMembers.class,
                                        "field1", "field2", "<init>()", "<init>(java.lang.String)", "method1()", "method2()"),
                                createMemberStrings(AnotherClassWithMembers.class, "field3", "<init>()", "method3()"))),
                $(Transformers.fields(),
                        Sets.union(
                                createMemberStrings(ClassWithMembers.class, "field1", "field2"),
                                createMemberStrings(AnotherClassWithMembers.class, "field3"))),
                $(Transformers.codeUnits(),
                        Sets.union(
                                createMemberStrings(ClassWithMembers.class,
                                        "<init>()", "<init>(java.lang.String)", "method1()", "method2()"),
                                createMemberStrings(AnotherClassWithMembers.class, "<init>()", "method3()"))),
                $(Transformers.methods(),
                        Sets.union(
                                createMemberStrings(ClassWithMembers.class, "method1()", "method2()"),
                                createMemberStrings(AnotherClassWithMembers.class, "method3()"))),
                $(Transformers.constructors(),
                        Sets.union(
                                createMemberStrings(ClassWithMembers.class, "<init>()", "<init>(java.lang.String)"),
                                createMemberStrings(AnotherClassWithMembers.class, "<init>()")))
        );
    }

    @Test
    @UseDataProvider("members_testcases")
    public void test_members(ClassesTransformer<JavaMember> transformer, Set<String> expectedMembers) {
        DescribedIterable<JavaMember> actualMembers = transformer.transform(importClasses(ClassWithMembers.class, AnotherClassWithMembers.class));

        assertThat(createMemberStrings(actualMembers)).isEqualTo(expectedMembers);
    }

    private static Set<String> createMemberStrings(Class<?> clazz, String... simpleMembers) {
        Set<String> result = new HashSet<>();
        for (String simpleMember : simpleMembers) {
            result.add(clazz.getName() + "." + simpleMember);
        }
        return result;
    }

    private static Set<String> createMemberStrings(Iterable<JavaMember> members) {
        Set<String> result = new HashSet<>();
        for (JavaMember member : members) {
            result.add(member.getFullName());
        }
        return result;
    }

    private static class ClassWithMembers {
        String field1;
        String field2;

        ClassWithMembers() {
        }

        ClassWithMembers(String field1) {
            this.field1 = field1;
        }

        void method1() {
        }

        void method2() {
        }
    }

    private static class AnotherClassWithMembers {
        String field3;

        AnotherClassWithMembers() {
        }

        void method3() {
        }
    }
}