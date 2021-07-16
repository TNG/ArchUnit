package com.tngtech.archunit.testutil.assertion;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.collect.Iterables.toArray;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.TestUtils.sortByName;
import static java.util.Arrays.sort;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaTypesAssertion extends AbstractObjectAssert<JavaTypesAssertion, JavaType[]> {
    @SuppressWarnings("ResultOfMethodCallIgnored") // the call modifies the object, even without using the result
    public JavaTypesAssertion(JavaType[] actual) {
        super(actual, JavaTypesAssertion.class);
        as("types");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // the call modifies the object, even without using the result
    public JavaTypesAssertion(Iterable<? extends JavaType> actual) {
        super(toArray(actual, JavaType.class), JavaTypesAssertion.class);
        as("types");
    }

    public void matchInAnyOrder(Iterable<Class<?>> classes) {
        assertThat(HasName.Utils.namesOf(actual)).as(descriptionText()).containsOnlyElementsOf(namesOf(classes));

        JavaType[] actualSorted = sortedJavaTypes(actual);
        Class<?>[] expectedSorted = sortedClasses(classes);
        for (int i = 0; i < actualSorted.length; i++) {
            assertThatType(actualSorted[i]).as("Element %d", i).matches(expectedSorted[i]);
        }
    }

    public void matchInAnyOrder(Class<?>... classes) {
        matchInAnyOrder(ImmutableList.copyOf(classes));
    }

    public void matchExactly(ExpectedConcreteType[] expected, DescriptionContext context) {
        matchExactly(ImmutableList.copyOf(expected), context);
    }

    public void matchExactly(List<ExpectedConcreteType> expected, DescriptionContext context) {
        assertThat(actual).as(context.describeElements(actual.length).toString()).hasSize(expected.size());
        for (int i = 0; i < actual.length; i++) {
            DescriptionContext elementContext = context.describeElement(i, actual.length);
            expected.get(i).assertMatchWith(actual[i], elementContext);
        }
    }

    public void matchExactly(Class<?>... classes) {
        assertThat(HasName.Utils.namesOf(actual)).as(descriptionText()).containsExactlyElementsOf(namesOf(classes));
        matchInAnyOrder(classes);
    }

    public JavaTypesAssertion contain(Class<?>... classes) {
        contain(ImmutableSet.copyOf(classes));
        return this;
    }

    public void doNotContain(Class<?>... classes) {
        assertThat(actualNames()).doesNotContainAnyElementsOf(namesOf(classes));
    }

    public void contain(Iterable<Class<?>> classes) {
        List<String> expectedNames = namesOf(Lists.newArrayList(classes));
        assertThat(actualNames()).as(descriptionText()).containsAll(expectedNames);
    }

    private Set<String> actualNames() {
        Set<String> actualNames = new HashSet<>();
        for (JavaType javaClass : actual) {
            actualNames.add(javaClass.getName());
        }
        return actualNames;
    }

    private JavaType[] sortedJavaTypes(JavaType[] javaTypes) {
        JavaType[] result = Arrays.copyOf(javaTypes, javaTypes.length);
        sortByName(result);
        return result;
    }

    private Class<?>[] sortedClasses(Iterable<Class<?>> classes) {
        Class<?>[] sorted = toArray(classes, Class.class);
        sort(sorted, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return sorted;
    }
}
