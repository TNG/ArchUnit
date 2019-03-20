package com.tngtech.archunit.testutil.assertion;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.JavaPackage.Functions.GET_RELATIVE_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaPackagesAssertion extends AbstractObjectAssert<JavaPackagesAssertion, JavaPackage[]> {
    public JavaPackagesAssertion(Iterable<JavaPackage> javaPackages) {
        super(sort(javaPackages), JavaPackagesAssertion.class);
    }

    private static JavaPackage[] sort(Iterable<JavaPackage> actual) {
        JavaPackage[] result = Iterables.toArray(actual, JavaPackage.class);
        sortByName(result);
        return result;
    }

    public void containPackagesOf(Class<?>... classes) {
        Set<String> expectedNames = getExpectedNames(classes);
        assertThat(getActualNames()).containsAll(expectedNames);
    }

    private Set<String> getExpectedNames(Class<?>[] classes) {
        Set<String> expectedNames = new HashSet<>();
        for (Class<?> clazz : classes) {
            expectedNames.add(clazz.getPackage().getName());
        }
        return expectedNames;
    }

    public void containRelativeNames(String... relativeNames) {
        assertThat(getActualRelativeNames()).contains(relativeNames);
    }

    public void containNames(String... names) {
        assertThat(getActualNames()).contains(names);
    }

    public void containOnlyNames(String... names) {
        assertThat(getActualNames()).containsOnly(names);
    }

    private ImmutableSet<String> getActualNames() {
        return FluentIterable.from(actual).transform(toGuava(GET_NAME)).toSet();
    }

    private ImmutableSet<String> getActualRelativeNames() {
        return FluentIterable.from(actual).transform(toGuava(GET_RELATIVE_NAME)).toSet();
    }

    public static <T extends HasName> void sortByName(T[] result) {
        Arrays.sort(result, new Comparator<HasName>() {
            @Override
            public int compare(HasName o1, HasName o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }
}
